package fi.fmi.avi.archiver.logging;

import org.junit.jupiter.api.Test;

final class AbstractAppendingLoggableTest {
    @Test
    void satisfies_AppendingLoggable_contract() {
        final AbstractAppendingLoggable loggable = new TestAppendingLoggable("any log string");
        LoggableTests.assertAppendingLoggableContract(loggable);
    }

    static final class TestAppendingLoggable extends AbstractAppendingLoggable {
        private final String logString;

        TestAppendingLoggable(final String logString) {
            this.logString = logString;
        }

        @Override
        protected int estimateLogStringLength() {
            return logString.length();
        }

        @Override
        public void appendTo(final StringBuilder builder) {
            builder.append(logString);
        }
    }
}
