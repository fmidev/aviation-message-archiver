package fi.fmi.avi.archiver.spring.retry;

import static java.util.Objects.requireNonNull;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.handler.advice.RequestHandlerRetryAdvice;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.listener.RetryListenerSupport;
import org.springframework.retry.support.RetryTemplateBuilder;

public class RetryAdviceFactory extends RequestHandlerRetryAdvice {
    private static final Logger LOGGER = LoggerFactory.getLogger(RetryAdviceFactory.class);

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

    public static RequestHandlerRetryAdvice create(final String description, final Duration initialInterval, final Duration maxInterval, final int multiplier,
            final Duration timeout) {
        requireNonNull(description, "description");
        requireNonNull(initialInterval, "initialInterval");
        requireNonNull(maxInterval, "maxInterval");
        requireNonNull(timeout, "timeout");

        final ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(initialInterval.toMillis());
        backOffPolicy.setMultiplier(multiplier);
        backOffPolicy.setMaxInterval(maxInterval.toMillis());

        final RetryTemplateBuilder retryTemplateBuilder = new RetryTemplateBuilder();
        if (timeout.isZero()) {
            retryTemplateBuilder.infiniteRetry();
        } else {
            retryTemplateBuilder.withinMillis(timeout.toMillis());
        }
        retryTemplateBuilder.customBackoff(backOffPolicy);

        retryTemplateBuilder.withListener(new RetryListenerSupport() {
            @Override
            public <T, E extends Throwable> void close(final RetryContext context, final RetryCallback<T, E> callback, final Throwable throwable) {
                super.close(context, callback, throwable);
                if (context.getRetryCount() > 0 && throwable != null) {
                    LOGGER.error("{} retry attempts exhausted", description);
                }
            }

            @Override
            public <T, E extends Throwable> void onError(final RetryContext context, final RetryCallback<T, E> callback, final Throwable throwable) {
                super.onError(context, callback, throwable);
                LOGGER.error("{} failed. Retry attempt {}", description, context.getRetryCount(), throwable);
            }
        });

        final RequestHandlerRetryAdvice retryAdvice = new RequestHandlerRetryAdvice();
        retryAdvice.setRetryTemplate(retryTemplateBuilder.build());
        return retryAdvice;
    }

    public RequestHandlerRetryAdvice create(final String description) {
        return create(description, initialInterval, maxInterval, retryMultiplier, timeout);
    }
}

