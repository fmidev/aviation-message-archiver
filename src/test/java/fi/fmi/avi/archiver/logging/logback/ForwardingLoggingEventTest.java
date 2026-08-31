package fi.fmi.avi.archiver.logging.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.google.common.testing.ForwardingWrapperTester;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.jupiter.api.Test;

@SuppressFBWarnings("SIC_INNER_SHOULD_BE_STATIC_ANON")
final class ForwardingLoggingEventTest {
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
