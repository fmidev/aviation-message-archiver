package fi.fmi.avi.archiver.config.factory.postaction;

import fi.fmi.avi.archiver.message.processor.postaction.AbstractRetryingPostAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.listener.RetryListenerSupport;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.retry.support.RetryTemplateBuilder;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static fi.fmi.avi.archiver.logging.GenericStructuredLoggable.loggableValue;
import static fi.fmi.avi.archiver.spring.retry.ArchiverRetryContexts.*;
import static java.util.Objects.requireNonNull;

public class DefaultRetryParamsFactory implements RetryingPostActionFactories.RetryParamsFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultRetryParamsFactory.class);
    private static final String WORKER_THREAD_SUFFIX = "-Worker";

    private static RetryTemplate retryTemplate(final RetryingPostActionFactories.RetryConfig retryConfig, final String actionName,
                                               final List<Class<? extends Throwable>> retryOn) {
        final ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(retryConfig.initialInterval().orElse(Duration.ofMillis(500)).toMillis());
        backOffPolicy.setMultiplier(retryConfig.multiplier().orElse(2));
        backOffPolicy.setMaxInterval(retryConfig.maxInterval().orElse(Duration.ofMinutes(1)).toMillis());

        final RetryTemplateBuilder retryTemplateBuilder = new RetryTemplateBuilder();
        retryTemplateBuilder.customBackoff(backOffPolicy);
        if (retryConfig.timeout().isPositive()) {
            retryTemplateBuilder.withinMillis(retryConfig.timeout().toMillis());
        } else {
            retryTemplateBuilder.infiniteRetry();
        }

        if (!retryOn.isEmpty()) {
            retryTemplateBuilder.retryOn(retryOn);
        }

        retryTemplateBuilder.withListener(new RetryLogger(actionName));
        return retryTemplateBuilder.build();
    }

    private static ThreadPoolExecutor actionExecutor(final int actionQueueCapacity, final String actionName) {
        final String workerThreadName = actionName + WORKER_THREAD_SUFFIX;
        return new ThreadPoolExecutor(
                1, 1,
                0L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(actionQueueCapacity),
                runnable -> {
                    final Thread thread = new Thread(runnable, workerThreadName);
                    thread.setDaemon(true);
                    return thread;
                },
                (runnable, exec) -> {
                    if (runnable instanceof final AbstractRetryingPostAction<?>.RetryingRunnable retryingRunnable) {
                        LOGGER.error("{} queue full; skipping action for <{}>",
                                loggableValue(RETRY_ACTION, actionName), retryingRunnable.getLoggingContext());
                    } else {
                        LOGGER.error("{} queue full; skipping action task", loggableValue(RETRY_ACTION, actionName));
                    }
                }
        );
    }

    @Override
    public AbstractRetryingPostAction.RetryParams retryParams(
            final RetryingPostActionFactories.RetryConfig config, final String actionName, final Duration actionTimeout,
            final int actionQueueCapacity, final List<Class<? extends Throwable>> retryOn) {
        requireNonNull(config, "config");
        requireNonNull(actionName, "actionName");
        requireNonNull(actionTimeout, "actionTimeout");
        requireNonNull(retryOn, "retryOn");

        return new AbstractRetryingPostAction.RetryParams(
                actionExecutor(actionQueueCapacity, actionName),
                actionTimeout,
                retryTemplate(config, actionName, retryOn));
    }

    private static final class RetryLogger extends RetryListenerSupport {
        private static final Logger LOGGER = LoggerFactory.getLogger(RetryLogger.class);

        private final String actionName;

        private RetryLogger(final String actionName) {
            this.actionName = requireNonNull(actionName, "actionName");
        }

        @Override
        public <T, E extends Throwable> void close(final RetryContext context, final RetryCallback<T, E> callback, @Nullable final Throwable throwable) {
            super.close(context, callback, throwable);
            final int retryCount = context.getRetryCount();
            if (retryCount > 0) {
                if (throwable == null) {
                    LOGGER.info("{} attempt {} succeeded for <{}>.",
                            loggableValue(RETRY_ACTION, actionName),
                            loggableValue(RETRY_COUNT_LOGNAME, retryCount + 1),
                            LOGGING_CONTEXT.get(context));
                } else {
                    LOGGER.error("{} attempts (total {}) exhausted for <{}>.",
                            loggableValue(RETRY_ACTION, actionName),
                            loggableValue(RETRY_COUNT_LOGNAME, retryCount),
                            LOGGING_CONTEXT.get(context));
                }
            }
        }

        @Override
        public <T, E extends Throwable> void onError(final RetryContext context, final RetryCallback<T, E> callback, final Throwable throwable) {
            super.onError(context, callback, throwable);
            LOGGER.warn("{} failed on attempt {} for <{}>. Retrying.",
                    loggableValue(RETRY_ACTION, actionName),
                    loggableValue(RETRY_COUNT_LOGNAME, context.getRetryCount()),
                    LOGGING_CONTEXT.get(context), throwable);
        }
    }
}
