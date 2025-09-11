package fi.fmi.avi.archiver.message.processor.postaction;

import com.rabbitmq.client.amqp.Message;
import com.rabbitmq.client.amqp.Publisher;
import fi.fmi.avi.archiver.logging.model.ReadableLoggingContext;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.processor.MessageProcessorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

public class SwimRabbitMQPublisher extends AbstractRetryingPostAction<Publisher.Context> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SwimRabbitMQPublisher.class);

    private final Publisher amqpPublisher;
    private final Consumer<Publisher.Context> healthIndicator;

    public SwimRabbitMQPublisher(
            final RetryParams retryParams,
            final Publisher amqpPublisher,
            final Consumer<Publisher.Context> healthIndicator) {
        super(retryParams);
        this.amqpPublisher = requireNonNull(amqpPublisher, "amqpPublisher");
        this.healthIndicator = requireNonNull(healthIndicator, "healthIndicator");
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
}
