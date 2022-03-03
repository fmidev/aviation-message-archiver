package fi.fmi.avi.archiver.util;

import static org.assertj.core.api.Assertions.assertThat;

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
    void isKnownGeneratedClass_returns_true_for_known_generated_classes(@Nullable final Class<?> cls) {
        assertThat(GeneratedClasses.isKnownGenerated(cls)).isTrue();
    }

    @ParameterizedTest
    @MethodSource("ordinaryClasses")
    @NullSource
    void isKnownGeneratedClass_returns_false_for_non_known_generated_classes(@Nullable final Class<?> cls) {
        assertThat(GeneratedClasses.isKnownGenerated(cls)).isFalse();
    }

    @ParameterizedTest
    @MethodSource("autoValueClasses")
    void isAutoValueClass_returns_true_for_AutoValue_classes(@Nullable final Class<?> cls) {
        assertThat(GeneratedClasses.isAutoValueGenerated(cls)).isTrue();
    }

    @ParameterizedTest
    @MethodSource({ "ordinaryClasses", "freeBuilderClasses" })
    @NullSource
    void isAutoValueClass_returns_false_for_non_AutoValue_classes(@Nullable final Class<?> cls) {
        assertThat(GeneratedClasses.isAutoValueGenerated(cls)).isFalse();
    }

    @ParameterizedTest
    @MethodSource("freeBuilderClasses")
    void isFreeBuilderClass_returns_true_for_FreeBuilder_classes(@Nullable final Class<?> cls) {
        assertThat(GeneratedClasses.isFreeBuilderGenerated(cls)).isTrue();
    }

    @ParameterizedTest
    @MethodSource({ "ordinaryClasses", "autoValueClasses" })
    @NullSource
    void isFreeBuilderClass_returns_false_for_non_FreeBuilder_classes(@Nullable final Class<?> cls) {
        assertThat(GeneratedClasses.isFreeBuilderGenerated(cls)).isFalse();
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
