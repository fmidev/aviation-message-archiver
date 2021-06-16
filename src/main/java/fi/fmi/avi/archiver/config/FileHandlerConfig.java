package fi.fmi.avi.archiver.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.Transformer;
import org.springframework.integration.file.transformer.FileToStringTransformer;
import org.springframework.integration.handler.advice.RequestHandlerRetryAdvice;
import org.springframework.messaging.Message;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.io.File;
import java.time.Duration;

@Configuration
public class FileHandlerConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileHandlerConfig.class);

    @Value("${file-handler.charset}")
    private String charset;

    @Value("${file-handler.retry.initial-interval:PT0.5S}")
    private Duration initialInterval;

    @Value("${file-handler.retry.multiplier:2}")
    private int retryMultiplier;

    @Value("${file-handler.retry.max-attempts:3}")
    private int retryMaxAttempts;

    @Transformer(inputChannel = "processingChannel", outputChannel = "parserChannel", adviceChain = "fileReadingRetryAdvice")
    public Message<?> fileToStringTransformer(final Message<File> message) {
        final FileToStringTransformer transformer = new FileToStringTransformer();
        transformer.setCharset(charset);
        return transformer.transform(message);
    }

    @Bean
    public RequestHandlerRetryAdvice fileReadingRetryAdvice() {
        final ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(initialInterval.toMillis());
        backOffPolicy.setMultiplier(retryMultiplier);
        final RetryPolicy retryPolicy = new SimpleRetryPolicy(retryMaxAttempts);

        final RetryTemplate retryTemplate = new RetryTemplate();
        retryTemplate.setBackOffPolicy(backOffPolicy);
        retryTemplate.setRetryPolicy(retryPolicy);

        final RequestHandlerRetryAdvice advice = new RequestHandlerRetryAdvice() {
            @Override
            public <T, E extends Throwable> void close(final RetryContext context, final RetryCallback<T, E> callback, final Throwable throwable) {
                super.close(context, callback, throwable);
                if (context.getRetryCount() > 0) {
                    LOGGER.error("File reading retry attempts exhausted");
                }
            }

            @Override
            public <T, E extends Throwable> void onError(final RetryContext context, final RetryCallback<T, E> callback, final Throwable throwable) {
                super.onError(context, callback, throwable);
                LOGGER.error("File reading failed. Retry attempt " + context.getRetryCount(), throwable);
            }
        };

        advice.setRetryTemplate(retryTemplate);
        return advice;
    }

}
