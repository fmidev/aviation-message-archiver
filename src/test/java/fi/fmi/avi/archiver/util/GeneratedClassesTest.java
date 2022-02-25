package fi.fmi.avi.archiver.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.inferred.freebuilder.FreeBuilder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;

import com.google.auto.value.AutoValue;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings("UPM_UNCALLED_PRIVATE_METHOD")
class GeneratedClassesTest {
    private static Stream<Class<?>> ordinaryClasses() {
        return Stream.of(//
                Object.class, //
                OrdinaryClass.class, //
                AutoValueClass.class, //
                AutoValueClass.Builder.class, //
                FreeBuilderClass.class, //
                FreeBuilderClass.Builder.class);
    }

    private static Stream<Class<?>> freeBuilderClasses() {
        return Stream.of(//
                // Builder
                GeneratedClassesTest_FreeBuilderClass_Builder.class, //
                // Value
                new FreeBuilderClass.Builder().build().getClass(), //
                // Partial
                new FreeBuilderClass.Builder().buildPartial().getClass(), //
                // Partial.Builder
                new FreeBuilderClass.Builder().buildPartial().toBuilder().getClass() //
        );
    }

    private static Stream<Class<?>> autoValueClasses() {
        return Stream.of(//
                AutoValue_GeneratedClassesTest_AutoValueClass.class, //
                AutoValue_GeneratedClassesTest_AutoValueClass.Builder.class //
        );
    }

    @ParameterizedTest
    @MethodSource({ "autoValueClasses", "freeBuilderClasses" })
    void isKnownGeneratedClass_Class_returns_true_for_known_generated_classes(@Nullable final Class<?> cls) {
        assertThat(GeneratedClasses.isKnownGeneratedClass(cls)).isTrue();
    }

    @ParameterizedTest
    @MethodSource({ "autoValueClasses", "freeBuilderClasses" })
    void isKnownGeneratedClass_String_returns_true_for_known_generated_classes(@Nullable final Class<?> cls) {
        final String className = Optional.ofNullable(cls).map(Class::getName).orElse(null);
        assertThat(GeneratedClasses.isKnownGeneratedClass(className)).isTrue();
    }

    @ParameterizedTest
    @MethodSource("ordinaryClasses")
    @NullSource
    void isKnownGeneratedClass_Class_returns_false_for_non_known_generated_classes(@Nullable final Class<?> cls) {
        assertThat(GeneratedClasses.isKnownGeneratedClass(cls)).isFalse();
    }

    @ParameterizedTest
    @MethodSource("ordinaryClasses")
    @NullSource
    void isKnownGeneratedClass_String_returns_false_for_non_known_generated_classes(@Nullable final Class<?> cls) {
        final String className = Optional.ofNullable(cls).map(Class::getName).orElse(null);
        assertThat(GeneratedClasses.isKnownGeneratedClass(className)).isFalse();
    }

    @ParameterizedTest
    @MethodSource("autoValueClasses")
    void isAutoValueClass_Class_returns_true_for_AutoValue_classes(@Nullable final Class<?> cls) {
        assertThat(GeneratedClasses.isAutoValueClass(cls)).isTrue();
    }

    @ParameterizedTest
    @MethodSource("autoValueClasses")
    void isAutoValueClass_String_returns_true_for_AutoValue_classes(@Nullable final Class<?> cls) {
        final String className = Optional.ofNullable(cls).map(Class::getName).orElse(null);
        assertThat(GeneratedClasses.isAutoValueClass(className)).isTrue();
    }

    @ParameterizedTest
    @MethodSource({ "ordinaryClasses", "freeBuilderClasses" })
    @NullSource
    void isAutoValueClass_Class_returns_false_for_non_AutoValue_classes(@Nullable final Class<?> cls) {
        assertThat(GeneratedClasses.isAutoValueClass(cls)).isFalse();
    }

    @ParameterizedTest
    @MethodSource({ "ordinaryClasses", "freeBuilderClasses" })
    @NullSource
    void isAutoValueClass_String_returns_false_for_non_AutoValue_classes(@Nullable final Class<?> cls) {
        final String className = Optional.ofNullable(cls).map(Class::getName).orElse(null);
        assertThat(GeneratedClasses.isAutoValueClass(className)).isFalse();
    }

    @ParameterizedTest
    @MethodSource("freeBuilderClasses")
    void isFreeBuilderClass_Class_returns_true_for_FreeBuilder_classes(@Nullable final Class<?> cls) {
        assertThat(GeneratedClasses.isFreeBuilderClass(cls)).isTrue();
    }

    @ParameterizedTest
    @MethodSource("freeBuilderClasses")
    void isFreeBuilderClass_String_returns_true_for_FreeBuilder_classes(@Nullable final Class<?> cls) {
        final String className = Optional.ofNullable(cls).map(Class::getName).orElse(null);
        assertThat(GeneratedClasses.isFreeBuilderClass(className)).isTrue();
    }

    @ParameterizedTest
    @MethodSource({ "ordinaryClasses", "autoValueClasses" })
    @NullSource
    void isFreeBuilderClass_Class_returns_false_for_non_FreeBuilder_classes(@Nullable final Class<?> cls) {
        assertThat(GeneratedClasses.isFreeBuilderClass(cls)).isFalse();
    }

    @ParameterizedTest
    @MethodSource({ "ordinaryClasses", "autoValueClasses" })
    @NullSource
    void isFreeBuilderClass_String_returns_false_for_non_FreeBuilder_classes(@Nullable final Class<?> cls) {
        final String className = Optional.ofNullable(cls).map(Class::getName).orElse(null);
        assertThat(GeneratedClasses.isFreeBuilderClass(className)).isFalse();
    }

    static class OrdinaryClass {
    }

    @AutoValue
    static abstract class AutoValueClass {
        public static AutoValueClass.Builder builder() {
            return new AutoValue_GeneratedClassesTest_AutoValueClass.Builder();
        }

        @AutoValue.Builder
        abstract static class Builder {
            abstract AutoValueClass build();
        }
    }

    @FreeBuilder
    static abstract class FreeBuilderClass {
        abstract FreeBuilderClass.Builder toBuilder();

        static class Builder extends GeneratedClassesTest_FreeBuilderClass_Builder {
        }
    }
}
