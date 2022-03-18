package fi.fmi.avi.archiver.logging;

import static fi.fmi.avi.archiver.logging.StructuredLoggable.defaultStructureName;
import static org.assertj.core.api.Assertions.assertThat;

import org.inferred.freebuilder.FreeBuilder;
import org.junit.jupiter.api.Test;

import com.google.auto.value.AutoValue;

class StructuredLoggableTest {
    @Test
    void getStructureName_returns_class_name_starting_with_lower_case_letter() {
        class SimpleNamedLoggable extends TestStructuredLoggable {
        }
        assertThat(defaultStructureName(SimpleNamedLoggable.class)).isEqualTo("simpleNamedLoggable");
    }

    @Test
    void getStructureName_returns_one_letter_class_name_as_lower_case() {
        class L extends TestStructuredLoggable {
        }
        assertThat(defaultStructureName(L.class)).isEqualTo("l");
    }

    @Test
    void getStructureName_returns_name_for_anonymous_class() {
        final TestStructuredLoggable loggable = new TestStructuredLoggable() {
        };
        assertThat(defaultStructureName(loggable.getClass())).isEqualTo("structuredLoggableTest_1");
    }

    @Test
    void getStructureName_returns_name_of_FreeBuilder_declaration_class_starting_lower_case_for_value() {
        final FreeBuilderLoggable loggable = new FreeBuilderLoggable.Builder().build();
        assertThat(defaultStructureName(loggable.getClass())).isEqualTo("freeBuilderLoggable");
    }

    @Test
    void getStructureName_returns_name_of_FreeBuilder_declaration_class_starting_lower_case_for_partial() {
        final FreeBuilderLoggable loggable = new FreeBuilderLoggable.Builder().buildPartial();
        assertThat(defaultStructureName(loggable.getClass())).isEqualTo("freeBuilderLoggable");
    }

    @Test
    void getStructureName_returns_name_of_AutoValue_declaration_class_starting_lower_case() {
        final AutoValueLoggable loggable = AutoValueLoggable.newInstance();
        assertThat(defaultStructureName(loggable.getClass())).isEqualTo("autoValueLoggable");
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

        @Override
        public String getStructureName() {
            return "defaultStructureName";
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
