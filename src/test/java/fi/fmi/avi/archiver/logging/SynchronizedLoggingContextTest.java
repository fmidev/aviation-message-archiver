package fi.fmi.avi.archiver.logging;

import org.junit.jupiter.api.Test;

import com.google.common.testing.ForwardingWrapperTester;

@SuppressWarnings("UnstableApiUsage")
class SynchronizedLoggingContextTest {
    @Test
    void testForwarding() {
        new ForwardingWrapperTester().testForwarding(LoggingContext.class, SynchronizedLoggingContext::new);
    }
}
