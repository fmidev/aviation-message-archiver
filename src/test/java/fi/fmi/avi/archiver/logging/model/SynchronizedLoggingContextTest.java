package fi.fmi.avi.archiver.logging.model;

import com.google.common.testing.ForwardingWrapperTester;
import org.junit.jupiter.api.Test;

class SynchronizedLoggingContextTest {
    @Test
    void testForwarding() {
        new ForwardingWrapperTester().testForwarding(LoggingContext.class, SynchronizedLoggingContext::new);
    }
}
