package fi.fmi.avi.archiver.spring.convert;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings("UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR")
class NonEmptyStringConverterTest {

    private TestConverter converter;

    @BeforeEach
    void setUp() {
        converter = new TestConverter();
    }

    @Test
    public void returns_null_for_empty_by_default() {
        assertThat(converter.convert("")).isNull();
        assertThat(converter.getInvocations()).isEmpty();
    }

    @Test
    public void returns_custom_value_for_empty() {
        final String emptyValue = "I am an empty value";
        final TestConverterWithCustomEmptyValue converter = new TestConverterWithCustomEmptyValue(emptyValue);
        assertThat(converter.convert("")).isSameAs(emptyValue);
        assertThat(converter.getInvocations()).isEmpty();
        assertThat(converter.getEmptyInvocations()).isEqualTo(1);
    }

    @Test
    public void invokes_convertNonEmpty_for_non_empty_value() {
        final String value1 = "This is a value";
        final String value2 = "This is another value";
        assertThat(converter.convert(value1)).isEqualTo(TestConverter.PREFIX + value1);
        assertThat(converter.convert(value2)).isEqualTo(TestConverter.PREFIX + value2);
        assertThat(converter.getInvocations()).containsExactly(value1, value2);
    }

    static class TestConverter extends AbstractNonEmptyStringConverter<String> {
        public static final String PREFIX = "Converted: ";
        private final List<String> invocations = new ArrayList<>();

        TestConverter() {
        }

        @Override
        protected String convertNonEmpty(final String source) {
            invocations.add(source);
            return PREFIX + source;
        }

        public List<String> getInvocations() {
            return Collections.unmodifiableList(invocations);
        }
    }

    static class TestConverterWithCustomEmptyValue extends TestConverter {
        @Nullable
        private final String emptyValue;
        private int emptyInvocations = 0;

        TestConverterWithCustomEmptyValue(@Nullable final String emptyValue) {
            super();
            this.emptyValue = emptyValue;
        }

        @Nullable
        @Override
        protected String getEmptyValue() {
            emptyInvocations += 1;
            return emptyValue;
        }

        public int getEmptyInvocations() {
            return emptyInvocations;
        }
    }
}
