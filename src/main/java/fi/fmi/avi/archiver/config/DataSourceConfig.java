package fi.fmi.avi.archiver.config;

import java.time.Clock;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import fi.fmi.avi.archiver.database.DatabaseAccess;
import fi.fmi.avi.archiver.database.DatabaseService;
import fi.fmi.avi.archiver.database.RetryContextAttributes;
import fi.fmi.avi.archiver.logging.model.ReadableLoggingContext;

@Configuration
public class DataSourceConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceConfig.class);

    @Bean
    DatabaseAccess databaseAccess(final NamedParameterJdbcTemplate jdbcTemplate, final Clock clock, final RetryTemplate databaseAccessRetryTemplate,
            @Value("${datasource.schema}") final String schema) {
        return new DatabaseAccess(jdbcTemplate, clock, databaseAccessRetryTemplate, schema);
    }

    @Bean
    DatabaseService databaseService(final DatabaseAccess databaseAccess) {
        return new DatabaseService(databaseAccess);
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
    RetryTemplate databaseAccessRetryTemplate(@Value("${datasource.retry.initial-interval:PT0.5S}") final Duration initialInterval, //
            @Value("${datasource.retry.multiplier:2}") final int multiplier, //
            @Value("${datasource.retry.max-interval:PT1M}") final Duration maxInterval, //
            @Value("${datasource.retry.timeout:PT0S}") final Duration timeout) {
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
        retryTemplateBuilder.notRetryOn(NonTransientDataAccessException.class);

        retryTemplateBuilder.withListener(new RetryListenerSupport() {
            @Override
            public <T, E extends Throwable> void close(final RetryContext context, final RetryCallback<T, E> callback, final Throwable throwable) {
                super.close(context, callback, throwable);
                final int retryCount = context.getRetryCount();
                if (retryCount > 0 && throwable != null && !NonTransientDataAccessException.class.isAssignableFrom(throwable.getClass())) {
                    LOGGER.error("Database operation retry attempts ({}) exhausted while processing <{}>.", retryCount,
                            RetryContextAttributes.getLoggingContext(context));
                }
            }

            @Override
            public <T, E extends Throwable> void onError(final RetryContext context, final RetryCallback<T, E> callback, final Throwable throwable) {
                super.onError(context, callback, throwable);
                final ReadableLoggingContext loggingContext = RetryContextAttributes.getLoggingContext(context);
                if (!NonTransientDataAccessException.class.isAssignableFrom(throwable.getClass())) {
                    LOGGER.error("Database operation failed while processing <{}>. Retry attempt {}.", loggingContext, context.getRetryCount(), throwable);
                } else if (throwable instanceof EmptyResultDataAccessException) {
                    LOGGER.debug("Empty result while processing <{}>: {}", loggingContext, throwable.getMessage());
                } else {
                    LOGGER.error("Database operation failed while processing <{}>. Not retrying.", loggingContext, throwable);
                }
            }
        });

        return retryTemplateBuilder.build();
    }

}
