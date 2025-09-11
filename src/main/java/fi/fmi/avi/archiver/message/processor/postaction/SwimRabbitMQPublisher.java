package fi.fmi.avi.archiver.message.processor.postaction;

import com.rabbitmq.client.amqp.Message;
import com.rabbitmq.client.amqp.Publisher;
import fi.fmi.avi.archiver.logging.model.ReadableLoggingContext;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.processor.MessageProcessorContext;
import fi.fmi.avi.model.AviationWeatherMessage;
import org.inferred.freebuilder.FreeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.zip.GZIPOutputStream;

import static java.util.Objects.requireNonNull;

public class SwimRabbitMQPublisher extends AbstractRetryingPostAction<Publisher.Context> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SwimRabbitMQPublisher.class);
    private static final String CONTENT_TYPE = "application/xml";
    private static final ContentEncoding FALLBACK_ENCODING = ContentEncoding.IDENTITY;

    private final String instanceId;
    private final Publisher amqpPublisher;
    private final Consumer<Publisher.Context> healthIndicator;
    private final Clock clock;
    private final int iwxxmFormatId;
    private final Map<Integer, String> subjectByMessageTypeId;
    private final MessageConfig messageConfig;

    public SwimRabbitMQPublisher(
            final RetryParams retryParams,
            final String instanceId,
            final Publisher amqpPublisher,
            final Consumer<Publisher.Context> healthIndicator,
            final Clock clock,
            final int iwxxmFormatId,
            final Map<Integer, String> subjectByMessageTypeId,
            final MessageConfig messageConfig) {
        super(retryParams);
        this.instanceId = requireNonNull(instanceId, "instanceId");
        this.amqpPublisher = requireNonNull(amqpPublisher, "amqpPublisher");
        this.healthIndicator = requireNonNull(healthIndicator, "healthIndicator");
        this.clock = requireNonNull(clock, "clock");
        this.iwxxmFormatId = iwxxmFormatId;
        this.subjectByMessageTypeId = requireNonNull(subjectByMessageTypeId, "subjectByMessageTypeId");
        this.messageConfig = requireNonNull(messageConfig, "messageConfig");
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' + instanceId + ')';
    }

    private static AviationWeatherMessage.ReportStatus getReportStatus(final ArchiveAviationMessage archiveAviationMessage) {
        final String version = archiveAviationMessage.getVersion().orElse("");
        if (version.startsWith("AA")) {
            return AviationWeatherMessage.ReportStatus.AMENDMENT;
        } else if (version.startsWith("CC")) {
            return AviationWeatherMessage.ReportStatus.CORRECTION;
        } else {
            return AviationWeatherMessage.ReportStatus.NORMAL;
        }
    }

    @Override
    public Future<Publisher.Context> runAsynchronously(final MessageProcessorContext context, final ArchiveAviationMessage message) {
        requireNonNull(context, "context");
        requireNonNull(message, "message");

        final Message amqpMessage = amqpPublisher
                .message(message.getMessage().getBytes(StandardCharsets.UTF_8))
                .messageId(1L);

        final CompletableFuture<Publisher.Context> future = new CompletableFuture<>();
        amqpPublisher.publish(amqpMessage, publisherContext -> {
            try {
                healthIndicator.accept(publisherContext);
            } catch (final RuntimeException runtimeException) {
                LOGGER.error("Health indicator threw while updating message <{}> status", context.getLoggingContext(), runtimeException);
            } catch (final Error error) {
                LOGGER.error("Fatal error in health indicator while updating message <{}> status", context.getLoggingContext(), error);
                throw error;
            } finally {
                future.complete(publisherContext);
            }
        });
        return future;
    }

    @Override
    public void run(final MessageProcessorContext context, final ArchiveAviationMessage message) {
        final ReadableLoggingContext loggingContext = context.getLoggingContext();

        if (message.getFormat() != iwxxmFormatId) {
            LOGGER.debug("Message <{}> is not in IWXXM format, format=<{}>. Skipping.", loggingContext, message.getFormat());
            return;
        }
        if (!subjectByMessageTypeId.containsKey(message.getType())) {
            LOGGER.debug("Message <{}> of type <{}> is not any of supported types. Skipping.", loggingContext, message.getType());
            return;
        }

        final Message amqpMessage = constructAmqpMessage(message, loggingContext);
        publishAmqpMessage(amqpMessage, loggingContext);
    }

    private Message constructAmqpMessage(final ArchiveAviationMessage archiveMessage, final ReadableLoggingContext loggingContext) {
        final Message amqpMessage = initMessage(archiveMessage.getMessage(), loggingContext)
                .priority(messageConfig.getPriority(archiveMessage.getType(), getReportStatus(archiveMessage)))
                .subject(requireNonNull(subjectByMessageTypeId.get(archiveMessage.getType())));
        messageConfig.getAbsoluteExpiryTime(amqpMessage.creationTime())
                .ifPresent(amqpMessage::absoluteExpiryTime);
        return amqpMessage;
    }

    private Message initMessage(final String message, final ReadableLoggingContext loggingContext) {
        final ContentEncoding requestedEncoding = messageConfig.getEncoding();
        byte[] messageBody;
        ContentEncoding actualEncoding;
        try {
            messageBody = requestedEncoding.encode(message);
            actualEncoding = requestedEncoding;
        } catch (final IOException exception) {
            LOGGER.warn("Failed to encode message {} as {}. Falling back to {} encoding.",
                    loggingContext, requestedEncoding.value(), FALLBACK_ENCODING.value(), exception);
            try {
                messageBody = FALLBACK_ENCODING.encode(message);
                actualEncoding = FALLBACK_ENCODING;
            } catch (final IOException failingException) {
                throw new IllegalStateException("Failed to encode message.", failingException);
            }
        }
        return amqpPublisher
                .message(messageBody)
                .messageId(UUID.randomUUID())
                .contentType(CONTENT_TYPE)
                .contentEncoding(actualEncoding.value())
                .creationTime(clock.millis());
    }

    private void publishAmqpMessage(final Message amqpMessage, final ReadableLoggingContext loggingContext) {
        amqpPublisher.publish(amqpMessage, publisherContext -> {
            healthIndicator.accept(publisherContext);
            if (publisherContext.status() == Publisher.Status.ACCEPTED) {
                LOGGER.debug("Published message <{}>.", loggingContext);
            } else if (publisherContext.failureCause() != null) {
                final Throwable failureCause = publisherContext.failureCause();
                LOGGER.error("Failed to publish message <{}> with status: <{}>: {}", loggingContext, publisherContext.status(),
                        failureCause.getMessage(), failureCause);
            } else {
                LOGGER.error("Failed to publish message <{}> with status: <{}>.", loggingContext, publisherContext.status());
            }
        });
        return future;
    }

    @Override
    public void checkResult(final Publisher.Context result, final ReadableLoggingContext loggingContext) throws Exception {
        requireNonNull(result, "result");
        requireNonNull(loggingContext, "loggingContext");

        if (result.status() == Publisher.Status.ACCEPTED) {
            LOGGER.info("Published message <{}>.", loggingContext);
            return;
        }

        final Throwable failure = result.failureCause();
        if (failure instanceof final Exception exception) {
            throw exception;
        }
        if (failure instanceof final Error error) {
            throw error;
        }
        throw new IllegalStateException("AMQP publish failed with status " + result.status());
    }

    public enum ContentEncoding {
        IDENTITY {
            @Override
            byte[] encode(final String content) {
                return content.getBytes(StandardCharsets.UTF_8);
            }
        },
        GZIP {
            @Override
            byte[] encode(final String content) throws IOException {
                final byte[] contentByteArray = content.getBytes(StandardCharsets.UTF_8);
                try (final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(contentByteArray.length);
                     final GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream, contentByteArray.length)) {
                    gzipOutputStream.write(contentByteArray, 0, contentByteArray.length);
                    gzipOutputStream.close();
                    return byteArrayOutputStream.toByteArray();
                }
            }
        };

        private final String value;

        ContentEncoding() {
            this.value = name().toLowerCase(Locale.ROOT);
        }

        public String value() {
            return value;
        }

        abstract byte[] encode(final String content) throws IOException;
    }

    @FreeBuilder
    public static abstract class MessageConfig {
        public static final int DEFAULT_PRIORITY = 0;

        public static MessageConfig.Builder builder() {
            return new Builder()
                    .setEncoding(ContentEncoding.IDENTITY);
        }

        public abstract List<PriorityDescriptor> getPriorities();

        public byte getPriority(final int messageType, final AviationWeatherMessage.ReportStatus reportStatus) {
            return (byte) getPriorities().stream()
                    .filter(descriptor ->
                            (descriptor.getMessageType().isEmpty() || descriptor.getMessageType().getAsInt() == messageType)
                                    && (descriptor.getReportStatus().isEmpty() || descriptor.getReportStatus().get().equals(reportStatus)))
                    .mapToInt(PriorityDescriptor::getPriority)
                    .findFirst()
                    .orElse(DEFAULT_PRIORITY);
        }

        public abstract ContentEncoding getEncoding();

        public abstract Optional<Duration> getExpiryTime();

        public OptionalLong getAbsoluteExpiryTime(final long creationTime) {
            return getExpiryTime()
                    .map(expiryTime -> OptionalLong.of(creationTime + expiryTime.toMillis()))
                    .orElse(OptionalLong.empty());
        }

        public static class Builder extends SwimRabbitMQPublisher_MessageConfig_Builder {
            Builder() {
            }
        }

        @FreeBuilder
        public static abstract class PriorityDescriptor {
            public static PriorityDescriptor.Builder builder() {
                return new Builder();
            }

            abstract OptionalInt getMessageType();

            abstract Optional<AviationWeatherMessage.ReportStatus> getReportStatus();

            abstract int getPriority();

            public static class Builder extends SwimRabbitMQPublisher_MessageConfig_PriorityDescriptor_Builder {
                Builder() {
                }

                @Override
                public Builder setPriority(final int priority) {
                    if (priority < 0 || priority > 9) {
                        throw new IllegalArgumentException("Invalid priority: <%d>; must be between 0-9".formatted(priority));
                    }
                    return super.setPriority(priority);
                }
            }
        }
    }

}
