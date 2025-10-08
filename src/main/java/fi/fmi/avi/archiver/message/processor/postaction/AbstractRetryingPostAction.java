package fi.fmi.avi.archiver.message.processor.postaction;

import fi.fmi.avi.archiver.logging.model.ReadableLoggingContext;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.processor.MessageProcessorContext;
import fi.fmi.avi.archiver.spring.retry.ArchiverRetryContexts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.support.RetryTemplate;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.concurrent.*;

import static java.util.Objects.requireNonNull;

public abstract class AbstractRetryingPostAction<T> implements PostAction, AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractRetryingPostAction.class);

    private final ExecutorService executor;
    private final Duration postActionTimeout;
    private final RetryTemplate retryTemplate;

    protected AbstractRetryingPostAction(final RetryParams retryParams) {
        requireNonNull(retryParams, "retryParams");
        this.executor = retryParams.executor();
        this.retryTemplate = retryParams.retryTemplate();
        this.postActionTimeout = retryParams.postActionTimeout();
    }

    @Override
    public final void run(final MessageProcessorContext context, final ArchiveAviationMessage message) {
        executor.execute(new RetryingRunnable(context, message));
    }

    @Override
    public final void close() throws Exception {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                LOGGER.warn("Executor <{}> for <{}> did not terminate cleanly.", executor, this);
                executor.shutdownNow();
            }
        } catch (final InterruptedException ie) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        closeResources();
    }

    /**
     * This method will be invoked upon {@link #close()}.
     * Subclasses may override this method to clean up resources.
     *
     * @throws Exception if resources cannot be closed
     */
    @SuppressWarnings("RedundantThrows")
    protected void closeResources() throws Exception {
    }

    /**
     * Like {@link PostAction#run(MessageProcessorContext, ArchiveAviationMessage)}, but enables asynchronous execution.
     *
     * @param context context
     * @param message message
     * @return future
     */
    protected abstract Future<T> runAsynchronously(MessageProcessorContext context, ArchiveAviationMessage message);

    /**
     * Check the result returned by {@link #runAsynchronously(MessageProcessorContext, ArchiveAviationMessage)}.
     * In case the result is faulty and requires retry, this method throws an exception.
     *
     * @param result         result of {@link #runAsynchronously(MessageProcessorContext, ArchiveAviationMessage)} execution
     * @param loggingContext logging context
     * @throws Exception if retry is required
     */
    protected abstract void checkResult(@Nullable T result, ReadableLoggingContext loggingContext) throws Exception;

    public record RetryParams(
            ExecutorService executor,
            Duration postActionTimeout,
            RetryTemplate retryTemplate) {
        public RetryParams {
            requireNonNull(executor, "executor");
            requireNonNull(postActionTimeout, "postActionTimeout");
            requireNonNull(retryTemplate, "retryTemplate");
        }
    }

    public final class RetryingRunnable implements Runnable {
        private final MessageProcessorContext messageProcessorContext;
        private final ArchiveAviationMessage message;

        private RetryingRunnable(final MessageProcessorContext messageProcessorContext, final ArchiveAviationMessage message) {
            this.messageProcessorContext = requireNonNull(messageProcessorContext, "messageProcessorContext");
            this.message = requireNonNull(message, "message");
        }

        public ReadableLoggingContext getLoggingContext() {
            return messageProcessorContext.getLoggingContext();
        }

        @Override
        public void run() {
            final ReadableLoggingContext loggingContext = getLoggingContext();
            try {
                retryTemplate.execute(retryContext -> {
                    ArchiverRetryContexts.LOGGING_CONTEXT.set(retryContext, loggingContext);
                    return retryCallback().doWithRetry(retryContext);
                }, retryContext -> {
                    LOGGER.error("Exhausted retries for running <{}> on message <{}>",
                            this, loggingContext, retryContext.getLastThrowable());
                    return null;
                });
            } catch (final Exception e) {
                LOGGER.error("Uncaught exception when running <{}> on message <{}>", this, loggingContext, e);
            }
        }

        private RetryCallback<Void, Exception> retryCallback() {
            return context -> {
                final Future<T> future = runAsynchronously(messageProcessorContext, message);
                final T result;
                try {
                    result = future.get(postActionTimeout.toMillis(), TimeUnit.MILLISECONDS);
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
                checkResult(result, getLoggingContext());
                return null;
            };
        }
    }
}
