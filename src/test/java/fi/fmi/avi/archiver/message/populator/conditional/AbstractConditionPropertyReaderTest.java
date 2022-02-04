package fi.fmi.avi.archiver.message.populator.conditional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.lang.reflect.Method;

import javax.annotation.Nullable;

import org.junit.jupiter.api.Test;

import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;

class AbstractConditionPropertyReaderTest {
    @Test
    void getValueGetterForType_returns_readValue_method() throws NoSuchMethodException {
        final Method expected = TestConditionPropertyReader.class.getMethod("readValue", InputAviationMessage.class, ArchiveAviationMessage.Builder.class);

        final Method result = new TestConditionPropertyReader().getValueGetterForType();

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void getValueGetterForType_throws_exception_if_readValue_returns_Object() {
        final class ObjectReader extends AbstractConditionPropertyReader<Object> {
            @Nullable
            @Override
            public Object readValue(final InputAviationMessage input, final ArchiveAviationMessage.Builder target) {
                return null;
            }
        }
        final ObjectReader reader = new ObjectReader();

        assertThatIllegalStateException().isThrownBy(reader::getValueGetterForType);
    }

    @Test
    void getPropertyName_returns_class_name_until_ConditionPropertyReader_starting_lower_case() {
        final class Test1PropertyConditionPropertyReader extends TestConditionPropertyReader {
        }
        assertThat(new Test1PropertyConditionPropertyReader().getPropertyName()).isEqualTo("test1Property");
    }

    @Test
    void getPropertyName_returns_class_name_until_words_of_ConditionPropertyReader_starting_lower_case1() {
        final class Test2PropertyPropertyReader extends TestConditionPropertyReader {
        }
        assertThat(new Test2PropertyPropertyReader().getPropertyName()).isEqualTo("test2Property");
    }

    @Test
    void getPropertyName_returns_class_name_until_words_of_ConditionPropertyReader_starting_lower_case2() {
        final class Test3PropertyReader extends TestConditionPropertyReader {
        }
        assertThat(new Test3PropertyReader().getPropertyName()).isEqualTo("test3");
    }

    @Test
    void getPropertyName_returns_class_name_until_words_of_ConditionPropertyReader_starting_lower_case3() {
        final class Test4PropertReader extends TestConditionPropertyReader {
        }
        assertThat(new Test4PropertReader().getPropertyName()).isEqualTo("test4Propert");
    }

    @Test
    void getPropertyName_returns_class_when_not_ending_in_words_of_ConditionPropertyReader_starting_lower_case() {
        final class Test5Propertyeader extends TestConditionPropertyReader {
        }
        assertThat(new Test5Propertyeader().getPropertyName()).isEqualTo("test5Propertyeader");
    }

    @Test
    void validate_returns_always_true() {
        assertThat(new TestConditionPropertyReader().validate("anyString")).isTrue();
    }

    private static class TestConditionPropertyReader extends AbstractConditionPropertyReader<String> {
        @Nullable
        @Override
        public String readValue(final InputAviationMessage input, final ArchiveAviationMessage.Builder target) {
            return "";
        }
    }
}
