package fi.fmi.avi.archiver.config;

import fi.fmi.avi.archiver.database.DatabaseAccess;
import fi.fmi.avi.archiver.database.DatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.NonTransientDataAccessException;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.messaging.MessageChannel;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.listener.RetryListenerSupport;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.retry.support.RetryTemplateBuilder;

import java.time.Clock;
import java.time.Duration;

@Configuration
public class DataSourceConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceConfig.class);

    @Value("${datasource.retry.initial-interval:PT0.5S}")
    private Duration retryInitialInterval;

    @Value("${datasource.retry.multiplier:2}")
    private int retryMultiplier;

    @Value("${datasource.retry.max-interval:PT1M}")
    private Duration retryMaxInterval;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    private Clock clock;

    @Autowired
    private MessageChannel databaseChannel;

    @Autowired
    private MessageChannel archiveChannel;

    @Bean
    public DatabaseAccess databaseAccess() {
        return new DatabaseAccess(jdbcTemplate, clock, databaseAccessRetryTemplate());
    }

    @Bean
    public DatabaseService databaseService() {
        return new DatabaseService(databaseAccess());
    }

    @Bean
    public IntegrationFlow databaseFlow() {
        return IntegrationFlows.from(databaseChannel)
                .handle(databaseService())
                .channel(archiveChannel)
                .get();
    }

    @Bean
    public RetryTemplate databaseAccessRetryTemplate() {
        final ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(retryInitialInterval.toMillis());
        backOffPolicy.setMultiplier(retryMultiplier);
        backOffPolicy.setMaxInterval(retryMaxInterval.toMillis());

        final RetryTemplateBuilder retryTemplateBuilder = new RetryTemplateBuilder();
        retryTemplateBuilder.infiniteRetry();
        retryTemplateBuilder.customBackoff(backOffPolicy);
        retryTemplateBuilder.notRetryOn(NonTransientDataAccessException.class);

        retryTemplateBuilder.withListener(new RetryListenerSupport() {
            @Override
            public <T, E extends Throwable> void close(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
                super.close(context, callback, throwable);
                if (context.getRetryCount() > 0 && !NonTransientDataAccessException.class.isAssignableFrom(throwable.getClass())) {
                    LOGGER.error("Database operation retry attempts exhausted");
                }
            }

            @Override
            public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
                super.onError(context, callback, throwable);
                if (!NonTransientDataAccessException.class.isAssignableFrom(throwable.getClass())) {
                    LOGGER.error("Database operation failed. Retry attempt " + context.getRetryCount(), throwable);
                }
            }
        });

        return retryTemplateBuilder.build();
    }

}
