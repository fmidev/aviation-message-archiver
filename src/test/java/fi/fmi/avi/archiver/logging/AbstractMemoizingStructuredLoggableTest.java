package fi.fmi.avi.archiver.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

final class AbstractMemoizingStructuredLoggableTest {
    @Test
    void subsequent_invocations_to_toString_do_not_delegate_to_toStringOnce() {
        final String logString = "any log string";
        final TestLoggable loggable = new TestLoggable(logString);

        assertThat(loggable.getInvocations()).isEqualTo(0);
        assertThat(loggable.toString()).isEqualTo(logString);
        assertThat(loggable.getInvocations()).isEqualTo(1);
        assertThat(loggable.toString()).isEqualTo(logString);
        assertThat(loggable.getInvocations()).isEqualTo(1);
        assertThat(loggable.toString()).isEqualTo(logString);
        assertThat(loggable.getInvocations()).isEqualTo(1);
    }

    @Test
    void readableCopy_returns_same_instance() {
        final TestLoggable loggable = new TestLoggable("log string");

        assertThat(loggable.readableCopy()).isSameAs(loggable);
    }

    @Test
    void getStructureName_returns_expected_name() {
        final TestLoggable loggable = new TestLoggable("log string");

        assertThat(loggable.getStructureName()).isEqualTo("testLoggable");
    }

    static final class TestLoggable extends AbstractMemoizingStructuredLoggable {
        private static final String STRUCTURE_NAME = StructuredLoggables.defaultStructureName(TestLoggable.class);
        private final String logString;
        private int invocations = 0;

        TestLoggable(final String logString) {
            this.logString = logString;
        }

        @Override
        protected void appendOnceTo(final StringBuilder builder) {
            invocations++;
            builder.append(logString);
        }

        public int getInvocations() {
            return invocations;
        }

        @Override
        public int estimateLogStringLength() {
            return logString.length();
        }

        @Override
        public String getStructureName() {
            return STRUCTURE_NAME;
        }
    }
}
