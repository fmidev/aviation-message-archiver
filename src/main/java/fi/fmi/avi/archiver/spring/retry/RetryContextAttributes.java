package fi.fmi.avi.archiver.spring.retry;

import static java.util.Objects.requireNonNull;

import javax.annotation.Nullable;

import org.springframework.retry.RetryContext;

import com.google.common.annotations.VisibleForTesting;

import fi.fmi.avi.archiver.logging.model.NoOpLoggingContext;
import fi.fmi.avi.archiver.logging.model.ReadableLoggingContext;

public final class RetryContextAttributes {
    @VisibleForTesting
    static final String LOGGING_CONTEXT_ATTR = ReadableLoggingContext.class.toString();

    private RetryContextAttributes() {
        throw new AssertionError();
    }

    public static ReadableLoggingContext getLoggingContext(final RetryContext retryContext) {
        requireNonNull(retryContext, "retryContext");
        final Object attribute = retryContext.getAttribute(LOGGING_CONTEXT_ATTR);
        return attribute instanceof ReadableLoggingContext ? (ReadableLoggingContext) attribute : NoOpLoggingContext.getInstance();
    }

    public static void setLoggingContext(final RetryContext retryContext, @Nullable final ReadableLoggingContext loggingContext) {
        requireNonNull(retryContext, "retryContext");
        retryContext.setAttribute(LOGGING_CONTEXT_ATTR, loggingContext == null ? null : loggingContext.readableCopy());
    }
}
