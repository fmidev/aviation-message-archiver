package fi.fmi.avi.archiver.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.inferred.freebuilder.FreeBuilder;
import org.junit.jupiter.api.Test;

import com.google.auto.value.AutoValue;

class StructuredLoggableTest {
    @Test
    void getStructureName_returns_class_name_starting_with_lower_case_letter() {
        class SimpleNamedLoggable extends TestStructuredLoggable {
        }
        assertThat(new SimpleNamedLoggable().getStructureName()).isEqualTo("simpleNamedLoggable");
    }

    @Test
    void getStructureName_returns_one_letter_class_name_as_lower_case() {
        class L extends TestStructuredLoggable {
        }
        assertThat(new L().getStructureName()).isEqualTo("l");
    }

    @Test
    void getStructureName_returns_name_for_anonymous_class() {
        final TestStructuredLoggable loggable = new TestStructuredLoggable() {
        };
        assertThat(loggable.getStructureName()).isEqualTo("structuredLoggableTest_1");
    }

    @Test
    void getStructureName_returns_name_of_FreeBuilder_declaration_class_starting_lower_case_for_value() {
        final FreeBuilderLoggable loggable = new FreeBuilderLoggable.Builder().build();
        assertThat(loggable.getStructureName()).isEqualTo("freeBuilderLoggable");
    }

    @Test
    void getStructureName_returns_name_of_FreeBuilder_declaration_class_starting_lower_case_for_partial() {
        final FreeBuilderLoggable loggable = new FreeBuilderLoggable.Builder().buildPartial();
        assertThat(loggable.getStructureName()).isEqualTo("freeBuilderLoggable");
    }

    @Test
    void getStructureName_returns_name_of_AutoValue_declaration_class_starting_lower_case() {
        final AutoValueLoggable loggable = AutoValueLoggable.newInstance();
        assertThat(loggable.getStructureName()).isEqualTo("autoValueLoggable");
    }

    private static abstract class TestStructuredLoggable implements StructuredLoggable {
        @Override
        public int estimateLogStringLength() {
            return 0;
        }

        @Override
        public TestStructuredLoggable readableCopy() {
            return this;
        }
    }

    @FreeBuilder
    static abstract class FreeBuilderLoggable extends TestStructuredLoggable {
        static class Builder extends StructuredLoggableTest_FreeBuilderLoggable_Builder {
        }
    }

    @AutoValue
    static abstract class AutoValueLoggable extends TestStructuredLoggable {
        static AutoValueLoggable newInstance() {
            return new AutoValue_StructuredLoggableTest_AutoValueLoggable();
        }
    }
}
