package fi.fmi.avi.archiver.config;

import fi.fmi.avi.archiver.spring.retry.RetryAdviceFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.Transformer;
import org.springframework.integration.file.transformer.FileToStringTransformer;
import org.springframework.integration.handler.advice.RequestHandlerRetryAdvice;
import org.springframework.messaging.Message;

import java.io.File;
import java.time.Duration;

@Configuration
public class FileHandlerConfig {

    @Value("${file-handler.charset}")
    private String charset;

    @Value("${file-handler.retry.initial-interval}")
    private Duration initialInterval;

    @Value("${file-handler.retry.max-interval}")
    private Duration maxInterval;

    @Value("${file-handler.retry.multiplier}")
    private int retryMultiplier;

    @Value("${file-handler.retry.timeout}")
    private Duration timeout;

    @Transformer(inputChannel = "processingChannel", outputChannel = "parserChannel", adviceChain = "fileReadingRetryAdvice")
    public Message<?> fileToStringTransformer(final Message<File> message) {
        final FileToStringTransformer transformer = new FileToStringTransformer();
        transformer.setCharset(charset);
        return transformer.transform(message);
    }

    @Bean
    public RequestHandlerRetryAdvice fileReadingRetryAdvice() {
        return RetryAdviceFactory.create("File reading", initialInterval, maxInterval, retryMultiplier, timeout);
    }

    @Bean
    public RequestHandlerRetryAdvice archiveRetryAdvice() {
        return RetryAdviceFactory.create("Writing to archive dir", initialInterval, maxInterval, retryMultiplier, timeout);
    }

    @Bean
    public RequestHandlerRetryAdvice failRetryAdvice() {
        return RetryAdviceFactory.create("Writing to fail dir", initialInterval, maxInterval, retryMultiplier, timeout);
    }

}
