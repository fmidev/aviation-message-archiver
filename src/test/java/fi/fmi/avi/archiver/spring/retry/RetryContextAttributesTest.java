package fi.fmi.avi.archiver.spring.retry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.retry.RetryContext;

import fi.fmi.avi.archiver.logging.model.NoOpLoggingContext;
import fi.fmi.avi.archiver.logging.model.ReadableLoggingContext;

class RetryContextAttributesTest {
    @Mock
    private RetryContext retryContext;
    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (mocks != null) {
            mocks.close();
        }
    }

    @Test
    void getLoggingContext_returns_NoOp_instance_if_not_set() {
        when(retryContext.getAttribute(any())).thenReturn(null);

        final ReadableLoggingContext result = RetryContextAttributes.getLoggingContext(retryContext);

        assertThat(result).isInstanceOf(NoOpLoggingContext.class);
    }

    @Test
    void getLoggingContext_returns_NoOp_instance_if_value_not_ReadableLoggingContext() {
        when(retryContext.getAttribute(any())).thenReturn(new Object());

        final ReadableLoggingContext result = RetryContextAttributes.getLoggingContext(retryContext);

        assertThat(result).isInstanceOf(NoOpLoggingContext.class);
    }

    @Test
    void getLoggingContext_returns_ReadableLoggingContext_when_set() {
        final ReadableLoggingContext loggingContext = mock(ReadableLoggingContext.class);
        when(retryContext.getAttribute(RetryContextAttributes.LOGGING_CONTEXT_ATTR)).thenReturn(loggingContext);

        final ReadableLoggingContext result = RetryContextAttributes.getLoggingContext(retryContext);

        assertThat(result).isSameAs(loggingContext);
    }

    @Test
    void setLoggingContext_accepts_null_value() {
        RetryContextAttributes.setLoggingContext(retryContext, null);

        verify(retryContext).setAttribute(RetryContextAttributes.LOGGING_CONTEXT_ATTR, null);
    }

    @Test
    void setLoggingContext_sets_readableCopy() {
        final ReadableLoggingContext loggingContext = mock(ReadableLoggingContext.class);
        final ReadableLoggingContext loggingContextCopy = mock(ReadableLoggingContext.class);
        when(loggingContext.readableCopy()).thenReturn(loggingContextCopy);
        when(loggingContextCopy.readableCopy()).thenReturn(loggingContextCopy);

        RetryContextAttributes.setLoggingContext(retryContext, loggingContext);

        verify(retryContext).setAttribute(RetryContextAttributes.LOGGING_CONTEXT_ATTR, loggingContextCopy);
    }
}
