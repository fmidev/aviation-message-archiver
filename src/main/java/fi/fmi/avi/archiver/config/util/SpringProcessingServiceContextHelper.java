package fi.fmi.avi.archiver.config.util;

import fi.fmi.avi.archiver.DefaultProcessingServiceContext;
import fi.fmi.avi.archiver.ProcessingServiceContext;
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
import java.util.function.BiFunction;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

public final class SpringProcessingServiceContextHelper {
    public static final MessageHeaderReference<ProcessingServiceContext> HEADER = MessageHeaderReference.simpleNameOf(ProcessingServiceContext.class);

    private static final Logger LOGGER = LoggerFactory.getLogger(SpringProcessingServiceContextHelper.class);

    private SpringProcessingServiceContextHelper() {
        throw new AssertionError();
    }

    private static ProcessingServiceContext logAndCreateFallbackContext() {
        LOGGER.warn("ProcessingServiceContext not available; missing header '{}'. Substituting with a fallback implementation.",
                HEADER);
        return new DefaultProcessingServiceContext(NoOpLoggingContext.getInstance());
    }

    public static ProcessingServiceContext getProcessingServiceContext(@Nullable final Message<?> message) {
        if (message == null) {
            return logAndCreateFallbackContext();
        }
        return getProcessingServiceContext(message.getHeaders());
    }

    public static ProcessingServiceContext getProcessingServiceContext(final MessageHeaders headers) {
        requireNonNull(headers, "headers");
        return HEADER.getOptional(headers)
                .orElseGet(SpringProcessingServiceContextHelper::logAndCreateFallbackContext);
    }

    public static GenericHandler<?> peekProcessingServiceContext(final Consumer<ProcessingServiceContext> consumer) {
        requireNonNull(consumer, "consumer");
        return ServiceActivators.peekHeader(HEADER, SpringProcessingServiceContextHelper::logAndCreateFallbackContext, consumer);
    }

    public static <P> GenericHandler<P> peekPayloadAndProcessingServiceContext(final BiConsumer<P, ProcessingServiceContext> consumer) {
        requireNonNull(consumer, "consumer");
        return ServiceActivators.peekPayloadAndHeader(HEADER, SpringProcessingServiceContextHelper::logAndCreateFallbackContext, consumer);
    }

    public static <PI, PO> GenericHandler<PI> mapPayloadWithProcessingServiceContext(final BiFunction<PI, ProcessingServiceContext, PO> action) {
        requireNonNull(action, "action");
        return ServiceActivators.mapPayloadWithHeader(HEADER, SpringProcessingServiceContextHelper::logAndCreateFallbackContext, action);
    }

    public static GenericHandler<?> peekLoggingContext(final Consumer<LoggingContext> consumer) {
        requireNonNull(consumer, "consumer");
        return ServiceActivators.peekHeader(HEADER, SpringProcessingServiceContextHelper::logAndCreateFallbackContext,
                context -> consumer.accept(context.getLoggingContext()));
    }

    public static <P> GenericHandler<P> peekPayloadAndLoggingContext(final BiConsumer<P, LoggingContext> consumer) {
        requireNonNull(consumer, "consumer");
        return ServiceActivators.peekPayloadAndHeader(HEADER, SpringProcessingServiceContextHelper::logAndCreateFallbackContext,
                (payload, context) -> consumer.accept(payload, context.getLoggingContext()));
    }

    public static <PI, PO> GenericHandler<PI> mapPayloadWithLoggingContext(final BiFunction<PI, LoggingContext, PO> action) {
        requireNonNull(action, "action");
        return ServiceActivators.mapPayloadWithHeader(HEADER, SpringProcessingServiceContextHelper::logAndCreateFallbackContext,
                (payload, context) -> action.apply(payload, context.getLoggingContext()));
    }
}
