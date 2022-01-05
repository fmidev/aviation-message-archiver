package fi.fmi.avi.archiver.logging;

import org.junit.jupiter.api.Test;

final class AbstractLoggableTest {
    @Test
    void satisfies_Loggable_contract() {
        final AbstractLoggable loggable = new TestLoggable("test log string");
        LoggableTests.assertLoggableContract(loggable);
    }

    static final class TestLoggable extends AbstractLoggable {
        private final String logString;

        TestLoggable(final String logString) {
            this.logString = logString;
        }

        @Override
        public String logString() {
            return logString;
        }
    }
}
