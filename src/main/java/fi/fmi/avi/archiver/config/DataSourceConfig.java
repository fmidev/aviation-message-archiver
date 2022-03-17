package fi.fmi.avi.archiver.config;

import static fi.fmi.avi.archiver.logging.GenericStructuredLoggable.loggableValue;
import static fi.fmi.avi.archiver.spring.retry.ArchiverRetryContexts.DATABASE_OPERATION;
import static fi.fmi.avi.archiver.spring.retry.ArchiverRetryContexts.LOGGING_CONTEXT;
import static fi.fmi.avi.archiver.spring.retry.ArchiverRetryContexts.RETRY_COUNT_LOGNAME;

import java.time.Clock;
import java.time.Duration;

import javax.annotation.Nullable;

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

@Configuration
public class DataSourceConfig {
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

        retryTemplateBuilder.withListener(new RetryLogger());

        return retryTemplateBuilder.build();
    }

    private static final class RetryLogger extends RetryListenerSupport {
        // When making changes to this class, check if equivalent changes are also needed in
        // fi.fmi.avi.archiver.spring.retry.RetryAdviceFactory.RetryLogger

        private static final Logger LOGGER = LoggerFactory.getLogger(RetryLogger.class);

        @Override
        public <T, E extends Throwable> void close(final RetryContext context, final RetryCallback<T, E> callback, @Nullable final Throwable throwable) {
            super.close(context, callback, throwable);
            final int retryCount = context.getRetryCount();
            if (retryCount > 0) {
                if (throwable == null) {
                    LOGGER.info("Database operation '{}' attempt {} succeeded with <{}>.", DATABASE_OPERATION.get(context),
                            loggableValue(RETRY_COUNT_LOGNAME, retryCount + 1), LOGGING_CONTEXT.get(context));
                } else if (!NonTransientDataAccessException.class.isAssignableFrom(throwable.getClass())) {
                    LOGGER.error("Database operation '{}' attempts (total {}) exhausted with <{}>.", DATABASE_OPERATION.get(context),
                            loggableValue(RETRY_COUNT_LOGNAME, retryCount), LOGGING_CONTEXT.get(context));
                }
            }
        }

        @Override
        public <T, E extends Throwable> void onError(final RetryContext context, final RetryCallback<T, E> callback, final Throwable throwable) {
            super.onError(context, callback, throwable);
            if (throwable instanceof NonTransientDataAccessException) {
                if (throwable instanceof EmptyResultDataAccessException) {
                    LOGGER.debug("Database operation '{}' returned empty result with <{}>: {};  Not retrying.", DATABASE_OPERATION.get(context),
                            LOGGING_CONTEXT.get(context), throwable.getMessage());
                } else {
                    LOGGER.error("Database operation '{}' failed with <{}>. Not retrying.", DATABASE_OPERATION.get(context), LOGGING_CONTEXT.get(context),
                            throwable);
                }
            } else {
                LOGGER.error("Database operation '{}' failed on attempt {} with <{}>. Retrying.", DATABASE_OPERATION.get(context),
                        loggableValue(RETRY_COUNT_LOGNAME, context.getRetryCount()), LOGGING_CONTEXT.get(context), throwable);
            }
        }
    }
}
