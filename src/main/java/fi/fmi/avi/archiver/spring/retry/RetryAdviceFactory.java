package fi.fmi.avi.archiver.spring.retry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.handler.advice.RequestHandlerRetryAdvice;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.listener.RetryListenerSupport;
import org.springframework.retry.support.RetryTemplateBuilder;

import java.time.Duration;

import static java.util.Objects.requireNonNull;

public class RetryAdviceFactory extends RequestHandlerRetryAdvice {

    private static final Logger LOGGER = LoggerFactory.getLogger(RetryAdviceFactory.class);

    public static RequestHandlerRetryAdvice create(final String description, final Duration initialInterval, final Duration maxInterval,
                                                   final int multiplier, final Duration timeout) {
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
            public <T, E extends Throwable> void close(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
                super.close(context, callback, throwable);
                if (context.getRetryCount() > 0 && throwable != null) {
                    LOGGER.error(description + " retry attempts exhausted");
                }
            }

            @Override
            public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
                super.onError(context, callback, throwable);
                LOGGER.error(description + " failed. Retry attempt " + context.getRetryCount(), throwable);
            }
        });

        final RequestHandlerRetryAdvice retryAdvice = new RequestHandlerRetryAdvice();
        retryAdvice.setRetryTemplate(retryTemplateBuilder.build());
        return retryAdvice;
    }
}

