package fi.fmi.avi.archiver.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

final class AbstractNoOpLoggableTest {

    @Test
    void satisfies_AppendingLoggable_contract() {
        final AbstractNoOpLoggable loggable = new TestNoOpLoggable("any log string");
        LoggableTests.assertAppendingLoggableContract(loggable);
    }

    @Test
    void logString_returns_default_String_when_not_overridden() {
        final AbstractNoOpLoggable loggable = new DefaultStringNoOpLoggable();
        assertThat(loggable.logString()).isEqualTo("[omitted]");
        LoggableTests.assertAppendingLoggableContract(loggable);
    }

    static final class DefaultStringNoOpLoggable extends AbstractNoOpLoggable {
    }

    static final class TestNoOpLoggable extends AbstractNoOpLoggable {
        private final String logString;

        TestNoOpLoggable(final String logString) {
            this.logString = logString;
        }

        @Override
        public String logString() {
            return logString;
        }
    }
}
