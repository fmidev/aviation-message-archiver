package fi.fmi.avi.archiver.config.util;

import fi.fmi.avi.archiver.logging.model.LoggingContext;
import fi.fmi.avi.archiver.logging.model.NoOpLoggingContext;
import fi.fmi.avi.archiver.spring.integration.dsl.ServiceActivators;
import fi.fmi.avi.archiver.spring.messaging.MessageHeaderReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.handler.GenericHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

import javax.annotation.Nullable;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

public final class SpringLoggingContextHelper {
    public static final MessageHeaderReference<LoggingContext> HEADER = MessageHeaderReference.simpleNameOf(LoggingContext.class);

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
        return HEADER.getOptional(headers)//
                .orElseGet(() -> {
                    LOGGER.warn("LoggingContext not available; missing header '{}'. Substituting with {}.", //
                            HEADER, NoOpLoggingContext.class.getSimpleName());
                    return NoOpLoggingContext.getInstance();
                });
    }

    public static GenericHandler<?> withLoggingContext(final Consumer<LoggingContext> consumer) {
        requireNonNull(consumer, "consumer");
        return ServiceActivators.peekHeader(HEADER, consumer);
    }

    public static <P> GenericHandler<P> withPayloadAndLoggingContext(final BiConsumer<P, LoggingContext> consumer) {
        requireNonNull(consumer, "consumer");
        return ServiceActivators.peekPayloadAndHeader(HEADER, consumer);
    }
}
