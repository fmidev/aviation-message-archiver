package fi.fmi.avi.archiver.message.processor.postaction;

import com.rabbitmq.client.amqp.Message;
import com.rabbitmq.client.amqp.Publisher;
import fi.fmi.avi.archiver.logging.model.ReadableLoggingContext;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.processor.MessageProcessorContext;
import fi.fmi.avi.archiver.spring.retry.ArchiverRetryContexts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.support.RetryTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

public final class SwimRabbitMQPublisher implements PostAction, AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(SwimRabbitMQPublisher.class);

    private final Publisher amqpPublisher;
    private final RetryTemplate retryTemplate;
    private final Consumer<Publisher.Context> healthIndicator;
    private final Duration publishTimeout;
    private final ThreadPoolExecutor executor;

    public SwimRabbitMQPublisher(
            final Publisher amqpPublisher,
            final ThreadPoolExecutor publishExecutor,
            final Duration publishTimeout,
            final RetryTemplate retryTemplate,
            final Consumer<Publisher.Context> healthIndicator) {
        this.amqpPublisher = requireNonNull(amqpPublisher, "amqpPublisher");
        this.executor = requireNonNull(publishExecutor, "publishExecutor");
        this.retryTemplate = requireNonNull(retryTemplate, "retryTemplate");
        this.healthIndicator = requireNonNull(healthIndicator, "healthIndicator");
        this.publishTimeout = requireNonNull(publishTimeout, "publishTimeout");
    }

    @Override
    public void run(final MessageProcessorContext context, final ArchiveAviationMessage message) {
        executor.execute(new PublishRunnable(message, context.getLoggingContext()));
    }

    @Override
    public void close() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                LOGGER.warn("Publisher executor did not terminate cleanly.");
                executor.shutdownNow();
            }
        } catch (final InterruptedException ie) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public final class PublishRunnable implements Runnable {
        private final ArchiveAviationMessage message;
        private final ReadableLoggingContext loggingContext;

        private PublishRunnable(final ArchiveAviationMessage message, final ReadableLoggingContext loggingContext) {
            this.message = requireNonNull(message, "message");
            this.loggingContext = requireNonNull(loggingContext, "loggingContext");
        }

        public ReadableLoggingContext getLoggingContext() {
            return loggingContext;
        }

        @Override
        public void run() {
            try {
                retryTemplate.execute(context -> {
                    ArchiverRetryContexts.LOGGING_CONTEXT.set(context, loggingContext);
                    return publish().doWithRetry(context);
                }, context -> {
                    LOGGER.error("Exhausted retries publishing message <{}>",
                            loggingContext, context.getLastThrowable());
                    return null;
                });
            } catch (final Exception e) {
                LOGGER.error("Uncaught exception publishing message <{}>", loggingContext, e);
            }
        }

        private RetryCallback<Void, Exception> publish() {
            return context -> {
                final Message amqpMessage = amqpPublisher
                        .message(message.getMessage().getBytes(StandardCharsets.UTF_8))
                        .messageId(1L); // TODO Set message ID

                final CompletableFuture<Publisher.Context> future = new CompletableFuture<>();
                amqpPublisher.publish(amqpMessage, publishContext -> {
                    try {
                        healthIndicator.accept(publishContext);
                    } catch (final RuntimeException runtimeException) {
                        LOGGER.error("Health indicator threw", runtimeException);
                    } catch (final Error error) {
                        LOGGER.error("Fatal error in health indicator", error);
                        throw error;
                    } finally {
                        future.complete(publishContext);
                    }
                });

                final Publisher.Context result;
                try {
                    result = future.get(publishTimeout.toMillis(), TimeUnit.MILLISECONDS);
                } catch (final InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    throw interruptedException;
                } catch (final ExecutionException executionException) {
                    final Throwable cause = executionException.getCause();
                    if (cause instanceof final Exception exception) {
                        throw exception;
                    }
                    if (cause instanceof final Error error) {
                        throw error;
                    }
                    throw executionException;
                }

                if (result.status() == Publisher.Status.ACCEPTED) {
                    LOGGER.info("Published message <{}>.", loggingContext);
                    return null;
                }

                final Throwable failure = result.failureCause();
                if (failure instanceof final Exception exception) {
                    throw exception;
                }
                if (failure instanceof final Error error) {
                    throw error;
                }
                throw new IllegalStateException("AMQP publish failed with status " + result.status());
            };
        }
    }

}