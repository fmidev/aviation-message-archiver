package fi.fmi.avi.archiver.logging;

import static java.util.Objects.requireNonNull;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.handler.GenericHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

import fi.fmi.avi.archiver.spring.integration.dsl.ServiceActivators;

public final class SpringLoggingContextHelper {
    public static final String HEADER_KEY = LoggingContext.class.getSimpleName();

    private static final Logger LOGGER = LoggerFactory.getLogger(SpringLoggingContextHelper.class);

    private SpringLoggingContextHelper() {
        throw new AssertionError();
    }

    public static LoggingContext getLoggingContext(@Nullable final Message<?> message) {
        if (message == null) {
            return NoOpLoggingContext.getInstance();
        }
        return getLoggingContext(message.getHeaders());
    }

    public static LoggingContext getLoggingContext(final MessageHeaders headers) {
        requireNonNull(headers, "headers");
        final LoggingContext loggingContext = headers.get(HEADER_KEY, LoggingContext.class);
        if (loggingContext == null) {
            LOGGER.warn("{} logging context not available; missing header '{}'. Substituting with {}.", //
                    LoggingContext.class.getSimpleName(), HEADER_KEY, NoOpLoggingContext.class.getSimpleName());
            return NoOpLoggingContext.getInstance();
        }
        return loggingContext;
    }

    public static GenericHandler<?> withLoggingContext(final Consumer<LoggingContext> consumer) {
        requireNonNull(consumer, "consumer");
        return ServiceActivators.peekHeader(LoggingContext.class, HEADER_KEY, consumer);
    }

    public static <P> GenericHandler<P> withLoggingContextAndPayload(final BiConsumer<LoggingContext, P> consumer) {
        requireNonNull(consumer, "consumer");
        return ServiceActivators.peekPayloadAndHeader(LoggingContext.class, HEADER_KEY, (payload, logger) -> consumer.accept(logger, payload));
    }
}
