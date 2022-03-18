package fi.fmi.avi.archiver.spring.retry;

import static fi.fmi.avi.archiver.spring.retry.ArchiverRetryContexts.LOGGING_CONTEXT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.retry.RetryContext;

import fi.fmi.avi.archiver.logging.model.ReadableLoggingContext;

class ArchiverRetryContextsTest {
    @Test
    void LOGGING_CONTEXT_set_accepts_null_value() {
        final RetryContext context = mock(RetryContext.class);

        LOGGING_CONTEXT.set(context, null);

        verify(context).setAttribute(LOGGING_CONTEXT.getName(), null);
    }

    @Test
    void LOGGING_CONTEXT_sets_readableCopy() {
        final ReadableLoggingContext loggingContext = mock(ReadableLoggingContext.class);
        final ReadableLoggingContext loggingContextCopy = mock(ReadableLoggingContext.class);
        when(loggingContext.readableCopy()).thenReturn(loggingContextCopy);
        when(loggingContextCopy.readableCopy()).thenReturn(loggingContextCopy);

        final ReadableLoggingContext result = LOGGING_CONTEXT.getDoOnSet().apply(loggingContext);

        assertThat(result).isEqualTo(loggingContextCopy);
    }
}
