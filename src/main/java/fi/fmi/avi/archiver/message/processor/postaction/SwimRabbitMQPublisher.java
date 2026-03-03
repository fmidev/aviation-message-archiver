package fi.fmi.avi.archiver.message.processor.postaction;

import com.google.common.collect.ImmutableSet;
import com.rabbitmq.client.amqp.Message;
import com.rabbitmq.client.amqp.Publisher;
import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.logging.model.ReadableLoggingContext;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.processor.MessageProcessorContext;
import fi.fmi.avi.model.AviationWeatherMessage;
import fi.fmi.avi.model.MessageType;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import org.inferred.freebuilder.FreeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
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
    private static final DateTimeFormatter RFC_3339_FORMAT = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneOffset.UTC);

    private final String instanceId;
    private final Publisher amqpPublisher;
    private final Consumer<Publisher.Context> healthIndicator;
    private final Clock clock;
    private final int iwxxmFormatId;
    private final Map<Integer, StaticApplicationProperties> staticAppPropsByTypeId;
    private final MessageConfig messageConfig;

    public SwimRabbitMQPublisher(
            final RetryParams retryParams,
            final String instanceId,
            final Publisher amqpPublisher,
            final Consumer<Publisher.Context> healthIndicator,
            final Clock clock,
            final int iwxxmFormatId,
            final Map<Integer, StaticApplicationProperties> staticAppPropsByTypeId,
            final MessageConfig messageConfig) {
        super(retryParams);
        this.instanceId = requireNonNull(instanceId, "instanceId");
        this.amqpPublisher = requireNonNull(amqpPublisher, "amqpPublisher");
        this.healthIndicator = requireNonNull(healthIndicator, "healthIndicator");
        this.clock = requireNonNull(clock, "clock");
        this.iwxxmFormatId = iwxxmFormatId;
        this.staticAppPropsByTypeId = requireNonNull(staticAppPropsByTypeId, "staticAppPropsByTypeId");
        this.messageConfig = requireNonNull(messageConfig, "messageConfig");
    }

    /**
     * Determine the report status of the message to be published.
     * <p>
     * If the input message's report status is {@link fi.fmi.avi.model.AviationWeatherMessage.ReportStatus#NORMAL},
     * we check the version string parsed from the bulletin heading. This fallback covers the cases where we are publishing
     * an IWXXM message with an IWXXM version that the conversion library does not yet support. In these cases the
     * input message's report status will be NORMAL regardless of the actual value in the IWXXM XML content.
     * </p>
     *
     * @param archiveAviationMessage archive message
     * @param inputAviationMessage   input message
     * @return report status
     */
    private static AviationWeatherMessage.ReportStatus getReportStatus(final ArchiveAviationMessage archiveAviationMessage,
                                                                       final InputAviationMessage inputAviationMessage) {
        if (inputAviationMessage.getMessage().getReportStatus() == AviationWeatherMessage.ReportStatus.NORMAL) {
            final String version = archiveAviationMessage.getVersion().orElse("");
            if (version.startsWith("AA")) {
                return AviationWeatherMessage.ReportStatus.AMENDMENT;
            } else if (version.startsWith("CC")) {
                return AviationWeatherMessage.ReportStatus.CORRECTION;
            } else {
                return AviationWeatherMessage.ReportStatus.NORMAL;
            }
        } else {
            return inputAviationMessage.getMessage().getReportStatus();
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' + instanceId + ')';
    }

    @Override
    public Future<Publisher.Context> runAsynchronously(final MessageProcessorContext context, final ArchiveAviationMessage message) {
        requireNonNull(context, "context");
        requireNonNull(message, "message");

        final ReadableLoggingContext loggingContext = context.getLoggingContext();
        if (message.getFormat() != iwxxmFormatId) {
            LOGGER.debug("Message <{}> is not in IWXXM format, format=<{}>. Skipping.", loggingContext, message.getFormat());
            return CompletableFuture.completedFuture(null);
        }

        if (!staticAppPropsByTypeId.containsKey(message.getType())) {
            LOGGER.debug("Message <{}> of type <{}> is not any of supported types. Skipping.", loggingContext, message.getType());
            return CompletableFuture.completedFuture(null);
        }

        if (context.getInputMessage().getMessage().isNil()) {
            LOGGER.debug("Message <{}> is a nil message. Skipping.", loggingContext);
            return CompletableFuture.completedFuture(null);
        }

        final Message amqpMessage = constructAmqpMessage(message, context);
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

    private Message constructAmqpMessage(final ArchiveAviationMessage archiveMessage, final MessageProcessorContext context) {
        final StaticApplicationProperties staticProps = staticAppPropsByTypeId.get(archiveMessage.getType());
        final Message amqpMessage = initMessage(archiveMessage.getMessage(), context.getLoggingContext())
                .priority(messageConfig.getPriority(archiveMessage.getType(), getReportStatus(archiveMessage, context.getInputMessage())))
                .subject(staticProps.subject())
                .toAddress().exchange(messageConfig.getExchange()).key(staticProps.subject()).message();
        messageConfig.getAbsoluteExpiryTime(amqpMessage.creationTime())
                .ifPresent(amqpMessage::absoluteExpiryTime);
        return setApplicationProperties(amqpMessage, archiveMessage, context, staticProps);
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

    private Message setApplicationProperties(final Message amqpMessage,
                                             final ArchiveAviationMessage archiveMessage,
                                             final MessageProcessorContext context,
                                             final StaticApplicationProperties staticApplicationProperties) {
        final ReadableLoggingContext loggingContext = context.getLoggingContext();
        final MessageType messageType = staticApplicationProperties.type();

        ApplicationProperty.REPORT_STATUS.set(amqpMessage, getReportStatus(archiveMessage, context.getInputMessage()).name(), messageType, loggingContext);
        ApplicationProperty.ICAO_LOCATION_IDENTIFIER.set(amqpMessage, archiveMessage.getStationIcaoCode(), messageType, loggingContext);
        ApplicationProperty.ISSUE_DATETIME.set(amqpMessage, RFC_3339_FORMAT.format(archiveMessage.getMessageTime()), messageType, loggingContext);
        ApplicationProperty.ICAO_LOCATION_TYPE.set(amqpMessage, staticApplicationProperties.icaoLocationType(), messageType, loggingContext);
        ApplicationProperty.CONFORMS_TO.set(amqpMessage, staticApplicationProperties.conformsTo(), messageType, loggingContext);

        ApplicationProperty.OBSERVATION_DATETIME.set(amqpMessage,
                context.getInputMessage().getMessage().getObservationTime()
                        .flatMap(PartialOrCompleteTimeInstant::getCompleteTime)
                        .map(RFC_3339_FORMAT::format)
                        .orElse(null),
                messageType, loggingContext);
        ApplicationProperty.START_DATETIME.set(amqpMessage,
                archiveMessage.getValidFrom().map(RFC_3339_FORMAT::format).orElse(null),
                messageType, loggingContext);
        ApplicationProperty.END_DATETIME.set(amqpMessage,
                archiveMessage.getValidTo().map(RFC_3339_FORMAT::format).orElse(null),
                messageType, loggingContext);

        return amqpMessage;
    }

    @Override
    public void checkResult(@Nullable final Publisher.Context result, final ReadableLoggingContext loggingContext) throws Exception {
        requireNonNull(loggingContext, "loggingContext");
        if (result == null) {
            return;
        }
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

    public enum ApplicationProperty {
        REPORT_STATUS("properties.report_status", Category.MANDATORY),
        ICAO_LOCATION_IDENTIFIER("properties.icao_location_identifier", Category.MANDATORY),
        ICAO_LOCATION_TYPE("properties.icao_location_type", Category.OPTIONAL),
        CONFORMS_TO("conformsTo", Category.CONDITIONAL),
        ISSUE_DATETIME("properties.issue_datetime", Category.MANDATORY),
        OBSERVATION_DATETIME("properties.datetime", Category.CONDITIONAL, ImmutableSet.of(MessageType.METAR, MessageType.SPECI)),
        START_DATETIME("properties.start_datetime", Category.CONDITIONAL, ImmutableSet.of(MessageType.TAF, MessageType.SIGMET)),
        END_DATETIME("properties.end_datetime", Category.CONDITIONAL, ImmutableSet.of(MessageType.TAF, MessageType.SIGMET));

        private final String key;
        private final Category category;
        private final Set<MessageType> requiredForTypes;

        ApplicationProperty(final String key, final Category category) {
            this(key, category, Collections.emptySet());
        }

        ApplicationProperty(final String key, final Category category, final Set<MessageType> requiredForTypes) {
            this.key = key;
            this.category = category;
            this.requiredForTypes = requiredForTypes;
        }

        public void set(final Message message,
                        @Nullable final String value,
                        final MessageType messageType,
                        final ReadableLoggingContext loggingContext) {
            final boolean required = category == Category.MANDATORY || (category == Category.CONDITIONAL && requiredForTypes.contains(messageType));
            if (value == null) {
                if (required) {
                    throw new IllegalArgumentException("Missing required property '" + key + "' for type " + messageType + " <" + loggingContext + '>');
                }
                return;
            }
            message.property(key, value);
        }

        public enum Category {MANDATORY, OPTIONAL, CONDITIONAL}
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

    public record StaticApplicationProperties(MessageType type, String subject, String conformsTo,
                                              String icaoLocationType) {
        public StaticApplicationProperties {
            requireNonNull(type, "type");
            requireNonNull(subject, "subject");
            requireNonNull(conformsTo, "conformsTo");
            requireNonNull(icaoLocationType, "icaoLocationType");
        }
    }

    @FreeBuilder
    public static abstract class MessageConfig {
        public static final int DEFAULT_PRIORITY = 0;

        public static MessageConfig.Builder builder() {
            return new Builder()
                    .setEncoding(ContentEncoding.IDENTITY);
        }

        public abstract String getExchange();

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

        public abstract Builder toBuilder();

        public static class Builder extends SwimRabbitMQPublisher_MessageConfig_Builder {
            Builder() {
            }
        }

        @FreeBuilder
        public static abstract class PriorityDescriptor {
            public static PriorityDescriptor.Builder builder() {
                return new Builder();
            }

            public abstract OptionalInt getMessageType();

            public abstract Optional<AviationWeatherMessage.ReportStatus> getReportStatus();

            public abstract int getPriority();

            public abstract Builder toBuilder();

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