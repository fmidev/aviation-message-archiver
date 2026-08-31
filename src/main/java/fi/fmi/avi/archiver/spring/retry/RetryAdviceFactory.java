package fi.fmi.avi.archiver.spring.retry;

import fi.fmi.avi.archiver.config.util.SpringProcessingServiceContextHelper;
import fi.fmi.avi.archiver.logging.model.LoggingContext;
import org.aopalliance.intercept.MethodInterceptor;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.handler.advice.RequestHandlerRetryAdvice;
import org.springframework.integration.support.ErrorMessageUtils;
import org.springframework.messaging.Message;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.retry.support.RetryTemplateBuilder;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static fi.fmi.avi.archiver.logging.GenericStructuredLoggable.loggableValue;
import static fi.fmi.avi.archiver.spring.retry.ArchiverRetryContexts.LOGGING_CONTEXT;
import static fi.fmi.avi.archiver.spring.retry.ArchiverRetryContexts.RETRY_COUNT_LOGNAME;
import static java.util.Objects.requireNonNull;

public class RetryAdviceFactory {
    private final Duration initialInterval;
    private final Duration maxInterval;
    private final int retryMultiplier;
    private final Duration timeout;

    public RetryAdviceFactory(final Duration initialInterval, final Duration maxInterval, final int retryMultiplier, final Duration timeout) {
        this.initialInterval = requireNonNull(initialInterval, "initialInterval");
        this.maxInterval = requireNonNull(maxInterval, "maxInterval");
        this.retryMultiplier = retryMultiplier;
        this.timeout = requireNonNull(timeout, "timeout");
    }

    public static MethodInterceptor create(final String description, final Duration initialInterval, final Duration maxInterval,
                                           final int retryMultiplier, final Duration timeout) {
        requireNonNull(description, "description");
        requireNonNull(initialInterval, "initialInterval");
        requireNonNull(maxInterval, "maxInterval");
        requireNonNull(timeout, "timeout");

        final RetryListenerRegistrarRequestHandlerRetryAdvice retryAdvice = new RetryListenerRegistrarRequestHandlerRetryAdvice();
        retryAdvice.registerListener(new RetryLogger(description));
        final ExponentialBackOffPolicy backOffPolicy = createBackOffPolicy(initialInterval, maxInterval, retryMultiplier);
        final RetryTemplate retryTemplate = createRetryTemplate(timeout, backOffPolicy);
        retryAdvice.setRetryTemplate(retryTemplate);
        return retryAdvice;
    }

    private static ExponentialBackOffPolicy createBackOffPolicy(final Duration initialInterval, final Duration maxInterval, final int retryMultiplier) {
        final ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(initialInterval.toMillis());
        backOffPolicy.setMultiplier(retryMultiplier);
        backOffPolicy.setMaxInterval(maxInterval.toMillis());
        return backOffPolicy;
    }

    private static RetryTemplate createRetryTemplate(
            final Duration timeout, final ExponentialBackOffPolicy backOffPolicy) {
        final RetryTemplateBuilder retryTemplateBuilder = new RetryTemplateBuilder()
                .customBackoff(backOffPolicy);
        if (timeout.isZero()) {
            retryTemplateBuilder.infiniteRetry();
        } else {
            retryTemplateBuilder.withTimeout(timeout);
        }
        return retryTemplateBuilder.build();
    }

    public MethodInterceptor create(final String description) {
        return create(description, initialInterval, maxInterval, retryMultiplier, timeout);
    }

    /**
     * A {@link RequestHandlerRetryAdvice} that also implements {@link RetryListenerRegistrar} registering all the
     * {@link RetryListener}s to the {@link RetryTemplate} only after it has registered itself as a
     * {@code RetryListener}.
     *
     * <p>
     * This class is needed because {@code RequestHandlerRetryAdvice} registers itself as a {@code RetryListener} to the
     * {@code RetryTemplate} it uses in its {@link #onInit()} method, which is called by Spring after the bean has been
     * initialized. If any other {@code RetryListener}s are registered to the {@code RetryTemplate} before that, they
     * will not be able to access the {@link Message} being processed in the {@link RetryContext} because the
     * {@code RequestHandlerRetryAdvice} has not yet registered itself and set the {@link LoggingContext} in the
     * {@link RetryContext}.
     * </p>
     */
    private static class RetryListenerRegistrarRequestHandlerRetryAdvice
            extends RequestHandlerRetryAdvice
            implements RetryListenerRegistrar {
        private final List<RetryListener> listeners = new ArrayList<>();

        private RetryTemplate retryTemplate;
        private boolean initialized; // = false

        public RetryListenerRegistrarRequestHandlerRetryAdvice() {
            setRetryTemplate(new RetryTemplate());
        }

        @Override
        public void setRetryTemplate(final RetryTemplate retryTemplate) {
            super.setRetryTemplate(retryTemplate);
            this.retryTemplate = retryTemplate;
        }

        @Override
        protected void onInit() {
            super.onInit();
            listeners.forEach(retryTemplate::registerListener);
            initialized = true;
        }

        @Override
        public void registerListener(final RetryListener listener) {
            requireNonNull(listener, "listener");
            listeners.add(listener);
            if (initialized) {
                retryTemplate.registerListener(listener);
            }
        }
    }

    private record RetryLogger(String description) implements RetryListener {
        // When making changes to this class, check if equivalent changes are also needed in
        // fi.fmi.avi.archiver.config.DataSourceConfig.RetryLogger

        private static final Logger LOGGER = LoggerFactory.getLogger(RetryLogger.class);

        @Override
        public <T, E extends Throwable> boolean open(final RetryContext context, final RetryCallback<T, E> callback) {
            if (context.getAttribute(ErrorMessageUtils.FAILED_MESSAGE_CONTEXT_KEY) instanceof final Message<?> message) {
                final LoggingContext loggingContext = SpringProcessingServiceContextHelper.getProcessingServiceContext(message).getLoggingContext();
                LOGGING_CONTEXT.set(context, loggingContext);
            }
            return true;
        }

        @Override
        public <T, E extends Throwable> void close(final RetryContext context, final RetryCallback<T, E> callback, final @Nullable Throwable throwable) {
            final int retryCount = context.getRetryCount();
            if (retryCount > 0) {
                if (throwable == null) {
                    LOGGER.info("{} attempt {} succeeded with <{}>.", description, loggableValue(RETRY_COUNT_LOGNAME, retryCount + 1),
                            LOGGING_CONTEXT.get(context));
                } else {
                    LOGGER.error("{} attempts (total {}) exhausted with <{}>.", description, loggableValue(RETRY_COUNT_LOGNAME, retryCount),
                            LOGGING_CONTEXT.get(context));
                }
            }
        }

        @Override
        public <T, E extends Throwable> void onError(final RetryContext context, final RetryCallback<T, E> callback, final Throwable throwable) {
            LOGGER.warn("{} failed on attempt {} with <{}>. Retrying.", description, loggableValue(RETRY_COUNT_LOGNAME, context.getRetryCount()),
                    LOGGING_CONTEXT.get(context), throwable);
        }
    }
}
