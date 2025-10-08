package fi.fmi.avi.archiver.util.instantiation;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static fi.fmi.avi.archiver.util.instantiation.OptionalType.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class OptionalTypeTest {
    public static Stream<Arguments> isOptional_returns_whether_provided_class_is_of_optional_type() {
        return Stream.of(
                arguments(OPTIONAL, Object.class, false),
                arguments(OPTIONAL, Optional.class, true),
                arguments(OPTIONAL, OptionalInt.class, false),
                arguments(OPTIONAL, OptionalLong.class, false),
                arguments(OPTIONAL, OptionalDouble.class, false),
                arguments(OPTIONAL_INT, Object.class, false),
                arguments(OPTIONAL_INT, Optional.class, false),
                arguments(OPTIONAL_INT, OptionalInt.class, true),
                arguments(OPTIONAL_INT, OptionalLong.class, false),
                arguments(OPTIONAL_INT, OptionalDouble.class, false),
                arguments(OPTIONAL_LONG, Object.class, false),
                arguments(OPTIONAL_LONG, Optional.class, false),
                arguments(OPTIONAL_LONG, OptionalInt.class, false),
                arguments(OPTIONAL_LONG, OptionalLong.class, true),
                arguments(OPTIONAL_LONG, OptionalDouble.class, false),
                arguments(OPTIONAL_DOUBLE, Object.class, false),
                arguments(OPTIONAL_DOUBLE, Optional.class, false),
                arguments(OPTIONAL_DOUBLE, OptionalInt.class, false),
                arguments(OPTIONAL_DOUBLE, OptionalLong.class, false),
                arguments(OPTIONAL_DOUBLE, OptionalDouble.class, true)
        );
    }

    @ParameterizedTest
    @MethodSource
    void isOptional_returns_whether_provided_class_is_of_optional_type(
            final OptionalType optionalType, final Class<?> input, final boolean expected) {
        assertThat(optionalType.isOptional(input)).isEqualTo(expected);
    }

    @ParameterizedTest
    @EnumSource(OptionalType.class)
    void empty_returns_empty_instance(final OptionalType optionalType) {
        final Object empty = optionalType.empty();
        switch (optionalType) {
            case OPTIONAL -> assertThat(empty)
                    .asInstanceOf(InstanceOfAssertFactories.OPTIONAL)
                    .isEmpty();
            case OPTIONAL_INT -> assertThat(empty)
                    .asInstanceOf(InstanceOfAssertFactories.OPTIONAL_INT)
                    .isEmpty();
            case OPTIONAL_LONG -> assertThat(empty)
                    .asInstanceOf(InstanceOfAssertFactories.OPTIONAL_LONG)
                    .isEmpty();
            case OPTIONAL_DOUBLE -> assertThat(empty)
                    .asInstanceOf(InstanceOfAssertFactories.OPTIONAL_DOUBLE)
                    .isEmpty();
            default -> fail("Unhandled optional type <%s> with empty value <%s> <%s>"
                    .formatted(optionalType, empty, empty.getClass()));
        }
    }

    @ParameterizedTest
    @CsvSource(textBlock = """
            OPTIONAL_INT, getOptionalInt, int
            OPTIONAL_LONG, getOptionalLong, long
            OPTIONAL_DOUBLE, getOptionalDouble, double
            """)
    void getValueType_returns_expected_value_type(final OptionalType optionalType, final String methodName, final Class<?> expectedValue) throws NoSuchMethodException {
        assertThat(optionalType.getValueType(optionalType.getOptionalClass()))
                .hasValue(expectedValue);
        final Method getOptionalMethodName = TestInterface.class.getMethod(methodName);
        assertThat(optionalType.getValueType(getOptionalMethodName.getGenericReturnType()))
                .hasValue(expectedValue);
    }

    @ParameterizedTest
    @CsvSource(textBlock = """
            OPTIONAL_INT, getOptionalInt, int
            OPTIONAL_LONG, getOptionalLong, long
            OPTIONAL_DOUBLE, getOptionalDouble, double
            """)
    void getAnyValueType_returns_expected_value_type(final OptionalType optionalType, final String methodName, final Class<?> expectedValue) throws NoSuchMethodException {
        assertThat(OptionalType.getAnyValueType(optionalType.getOptionalClass()))
                .hasValue(expectedValue);
        final Method getOptionalMethodName = TestInterface.class.getMethod(methodName);
        assertThat(OptionalType.getAnyValueType(getOptionalMethodName.getGenericReturnType()))
                .hasValue(expectedValue);
    }

    @ParameterizedTest
    @EnumSource(value = OptionalType.class, mode = EnumSource.Mode.EXCLUDE, names = {"OPTIONAL"})
    void getValueType_on_an_unknown_type_return_empty(final OptionalType optionalType) {
        assertThat(optionalType.getValueType(Optional.class)).isEmpty();
    }

    @Test
    void getAnyValueType_on_an_unknown_type_return_empty() {
        assertThat(OptionalType.getAnyValueType(String.class)).isEmpty();
    }

    @Test
    void getValueType_Optional_returns_Object_on_plain_class() {
        assertThat(OPTIONAL.getValueType(Optional.class)).hasValue(Object.class);
    }

    @Test
    void getAnyValueType_Optional_returns_Object_on_plain_class() {
        assertThat(OptionalType.getAnyValueType(Optional.class)).hasValue(Object.class);
    }

    @Test
    void getValueType_Optional_returns_generic_type() throws NoSuchMethodException {
        final Method getOptionalMethodName = TestInterface.class.getMethod("getOptionalString");
        assertThat(OPTIONAL.getValueType(getOptionalMethodName.getGenericReturnType())).hasValue(String.class);
    }

    @Test
    void getAnyValueType_Optional_returns_generic_type() throws NoSuchMethodException {
        final Method getOptionalMethodName = TestInterface.class.getMethod("getOptionalString");
        assertThat(OptionalType.getAnyValueType(getOptionalMethodName.getGenericReturnType())).hasValue(String.class);
    }

    @Test
    void getValueType_Optional_returns_empty_on_non_optional_parameterized_type() throws NoSuchMethodException {
        final Method getOptionalMethodName = TestInterface.class.getMethod("getStringConsumer");
        assertThat(OPTIONAL.getValueType(getOptionalMethodName.getGenericReturnType())).isEmpty();
    }

    @Test
    void getAnyValueType_Optional_returns_empty_on_non_optional_parameterized_type() throws NoSuchMethodException {
        final Method getOptionalMethodName = TestInterface.class.getMethod("getStringConsumer");
        assertThat(OptionalType.getAnyValueType(getOptionalMethodName.getGenericReturnType())).isEmpty();
    }

    private interface TestInterface {
        Consumer<String> getStringConsumer();

        Optional<String> getOptionalString();

        OptionalInt getOptionalInt();

        OptionalLong getOptionalLong();

        OptionalDouble getOptionalDouble();
    }
}
