package fi.fmi.avi.archiver.spring.retry;

import static fi.fmi.avi.archiver.logging.GenericStructuredLoggable.loggableValue;
import static fi.fmi.avi.archiver.spring.retry.ArchiverRetryContexts.LOGGING_CONTEXT;
import static fi.fmi.avi.archiver.spring.retry.ArchiverRetryContexts.RETRY_COUNT_LOGNAME;
import static java.util.Objects.requireNonNull;

import java.time.Duration;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.handler.advice.RequestHandlerRetryAdvice;
import org.springframework.integration.support.ErrorMessageUtils;
import org.springframework.messaging.Message;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.listener.RetryListenerSupport;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.retry.support.RetryTemplateBuilder;

import fi.fmi.avi.archiver.config.SpringLoggingContextHelper;
import fi.fmi.avi.archiver.logging.model.LoggingContext;

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

    public static RequestHandlerRetryAdvice create(final String description, final Duration initialInterval, final Duration maxInterval,
            final int retryMultiplier, final Duration timeout) {
        requireNonNull(description, "description");
        requireNonNull(initialInterval, "initialInterval");
        requireNonNull(maxInterval, "maxInterval");
        requireNonNull(timeout, "timeout");

        final RequestHandlerRetryAdvice retryAdvice = new RequestHandlerRetryAdvice();
        final ExponentialBackOffPolicy backOffPolicy = createBackOffPolicy(initialInterval, maxInterval, retryMultiplier);
        final RetryTemplate retryTemplate = createRetryTemplate(description, timeout, retryAdvice, backOffPolicy);
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

    private static RetryTemplate createRetryTemplate(final String description, final Duration timeout, final RequestHandlerRetryAdvice retryAdvice,
            final ExponentialBackOffPolicy backOffPolicy) {
        final RetryTemplateBuilder retryTemplateBuilder = new RetryTemplateBuilder();

        if (timeout.isZero()) {
            retryTemplateBuilder.infiniteRetry();
        } else {
            retryTemplateBuilder.withinMillis(timeout.toMillis());
        }
        retryTemplateBuilder.customBackoff(backOffPolicy);

        // retryAdvice must be the first listener for Message to be available in RetryContext.
        retryTemplateBuilder.withListener(retryAdvice);
        retryTemplateBuilder.withListener(new RetryLogger(description));

        return retryTemplateBuilder.build();
    }

    public RequestHandlerRetryAdvice create(final String description) {
        return create(description, initialInterval, maxInterval, retryMultiplier, timeout);
    }

    private static final class RetryLogger extends RetryListenerSupport {
        // When making changes to this class, check if equivalent changes are also needed in
        // fi.fmi.avi.archiver.config.DataSourceConfig.RetryLogger

        private static final Logger LOGGER = LoggerFactory.getLogger(RetryLogger.class);

        private final String description;

        public RetryLogger(final String description) {
            this.description = description;
        }

        @Override
        public <T, E extends Throwable> boolean open(final RetryContext context, final RetryCallback<T, E> callback) {
            final boolean returnValue = super.open(context, callback);
            @Nullable
            final Object message = context.getAttribute(ErrorMessageUtils.FAILED_MESSAGE_CONTEXT_KEY);
            if (message instanceof Message<?>) {
                final LoggingContext loggingContext = SpringLoggingContextHelper.getLoggingContext((Message<?>) message);
                LOGGING_CONTEXT.set(context, loggingContext);
            }
            return returnValue;
        }

        @Override
        public <T, E extends Throwable> void close(final RetryContext context, final RetryCallback<T, E> callback, @Nullable final Throwable throwable) {
            super.close(context, callback, throwable);
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
            super.onError(context, callback, throwable);
            LOGGER.warn("{} failed on attempt {} with <{}>. Retrying.", description, loggableValue(RETRY_COUNT_LOGNAME, context.getRetryCount()),
                    LOGGING_CONTEXT.get(context), throwable);
        }
    }
}
