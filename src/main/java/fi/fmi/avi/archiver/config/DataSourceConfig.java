package fi.fmi.avi.archiver.config;

import fi.fmi.avi.archiver.database.DatabaseAccess;
import fi.fmi.avi.archiver.database.DatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.NonTransientDataAccessException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
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

    @Value("${datasource.retry.timeout:PT0S}")
    private Duration retryTimeout;

    @Value("${datasource.schema}")
    private String schema;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    private Clock clock;

    @Bean
    public DatabaseAccess databaseAccess() {
        return new DatabaseAccess(jdbcTemplate, clock, databaseAccessRetryTemplate(), schema);
    }

    @Bean
    public DatabaseService databaseService() {
        return new DatabaseService(databaseAccess());
    }

    /**
     * Retry logic for database operations.
     * <p>
     * Database operations are retried in blocking manner. When a database operation fails and enters the retrying logic,
     * no further operations will be attempted until the retry succeeds or timeouts. The retry logic is not applied
     * for {@link NonTransientDataAccessException}s because they are known to not succeed on future attempts. Retrying
     * applies for query timeouts, connectivity issues and similar recoverable errors. The blocking approach is preferable
     * in these situations, because it is unlikely that a parallel operation would succeed while retrying another operation.
     * <p>
     * Retrying is done infinitely unless a timeout is configured.
     *
     * @return retry template for database access
     */
    @Bean
    public RetryTemplate databaseAccessRetryTemplate() {
        final ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(retryInitialInterval.toMillis());
        backOffPolicy.setMultiplier(retryMultiplier);
        backOffPolicy.setMaxInterval(retryMaxInterval.toMillis());

        final RetryTemplateBuilder retryTemplateBuilder = new RetryTemplateBuilder();
        if (retryTimeout.isZero()) {
            retryTemplateBuilder.infiniteRetry();
        } else {
            retryTemplateBuilder.withinMillis(retryTimeout.toMillis());
        }
        retryTemplateBuilder.customBackoff(backOffPolicy);
        retryTemplateBuilder.notRetryOn(NonTransientDataAccessException.class);

        retryTemplateBuilder.withListener(new RetryListenerSupport() {
            @Override
            public <T, E extends Throwable> void close(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
                super.close(context, callback, throwable);
                if (context.getRetryCount() > 0 && throwable != null
                        && !NonTransientDataAccessException.class.isAssignableFrom(throwable.getClass())) {
                    LOGGER.error("Database operation retry attempts exhausted");
                }
            }

            @Override
            public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
                super.onError(context, callback, throwable);
                if (!NonTransientDataAccessException.class.isAssignableFrom(throwable.getClass())) {
                    LOGGER.error("Database operation failed. Retry attempt " + context.getRetryCount(), throwable);
                } else if (throwable instanceof EmptyResultDataAccessException) {
                    LOGGER.debug("Empty result", throwable);
                } else {
                    LOGGER.error("Database operation failed. Not retrying", throwable);
                }
            }
        });

        return retryTemplateBuilder.build();
    }

}
