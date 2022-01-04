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

public final class SpringProcessingChainLoggerHelper {
    public static final String HEADER_KEY = ProcessingChainLogger.class.getSimpleName();

    private static final Logger LOGGER = LoggerFactory.getLogger(SpringProcessingChainLoggerHelper.class);

    private SpringProcessingChainLoggerHelper() {
        throw new AssertionError();
    }

    public static ProcessingChainLogger getLogger(@Nullable final Message<?> message) {
        if (message == null) {
            return NoOpProcessingChainLogger.getInstance();
        }
        return getLogger(message.getHeaders());
    }

    public static ProcessingChainLogger getLogger(final MessageHeaders headers) {
        requireNonNull(headers, "headers");
        final ProcessingChainLogger logger = headers.get(HEADER_KEY, ProcessingChainLogger.class);
        if (logger == null) {
            LOGGER.warn("{} logger not available; missing header '{}'. Substituting with {}.", //
                    ProcessingChainLogger.class.getSimpleName(), HEADER_KEY, NoOpProcessingChainLogger.class.getSimpleName());
            return NoOpProcessingChainLogger.getInstance();
        }
        return logger;
    }

    public static GenericHandler<?> withLogger(final Consumer<ProcessingChainLogger> consumer) {
        requireNonNull(consumer, "consumer");
        return ServiceActivators.peekHeader(ProcessingChainLogger.class, HEADER_KEY, consumer);
    }

    public static <P> GenericHandler<P> withLoggerAndPayload(final BiConsumer<ProcessingChainLogger, P> consumer) {
        requireNonNull(consumer, "consumer");
        return ServiceActivators.peekPayloadAndHeader(ProcessingChainLogger.class, HEADER_KEY, (payload, logger) -> consumer.accept(logger, payload));
    }
}
