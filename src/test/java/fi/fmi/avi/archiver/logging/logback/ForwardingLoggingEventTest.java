package fi.fmi.avi.archiver.logging.logback;

import org.junit.jupiter.api.Test;

import com.google.common.testing.ForwardingWrapperTester;

import ch.qos.logback.classic.spi.ILoggingEvent;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings("SIC_INNER_SHOULD_BE_STATIC_ANON")
final class ForwardingLoggingEventTest {
    @SuppressWarnings("UnstableApiUsage")
    @Test
    void testForwarding() {
        new ForwardingWrapperTester().testForwarding(ILoggingEvent.class, loggingEvent -> new ForwardingLoggingEvent() {
            @Override
            protected ILoggingEvent delegate() {
                return loggingEvent;
            }
        });
    }
}
