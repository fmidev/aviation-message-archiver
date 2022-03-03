package fi.fmi.avi.archiver.logging.logback;

import ch.qos.logback.core.spi.AppenderAttachable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

class ForwardingAppenderBaseTest extends ForwardingAppenderBaseTester<Object, AppenderAttachable<Object>> {
    @SuppressFBWarnings("SIC_INNER_SHOULD_BE_STATIC_ANON")
    @Override
    protected ForwardingAppenderBase<Object> createAppender(final AppenderAttachable<Object> delegateAppender) {
        return new ForwardingAppenderBase<Object>() {
            @Override
            protected AppenderAttachable<Object> appenders() {
                return delegateAppender;
            }
        };
    }
}
