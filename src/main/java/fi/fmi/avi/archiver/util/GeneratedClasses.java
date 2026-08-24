package fi.fmi.avi.archiver.util;

import org.jspecify.annotations.Nullable;

public final class GeneratedClasses {
    private GeneratedClasses() {
        throw new AssertionError();
    }

    public static boolean isKnownGenerated(final @Nullable Class<?> cls) {
        return cls != null && (isKnownGeneratedByName(cls.getName()) || isNativeGenerated(cls));
    }

    private static boolean isKnownGeneratedByName(final @Nullable String className) {
        return isAutoValueGenerated(className) || isFreeBuilderGenerated(className);
    }

    public static boolean isNativeGenerated(final @Nullable Class<?> cls) {
        return cls != null && cls.isRecord();
    }

    public static boolean isAutoValueGenerated(final @Nullable Class<?> cls) {
        return cls != null && isAutoValueGenerated(cls.getName());
    }

    private static boolean isAutoValueGenerated(final @Nullable String className) {
        return className != null && className.substring(className.lastIndexOf('.') + 1).startsWith("AutoValue_");
    }

    public static boolean isFreeBuilderGenerated(final @Nullable Class<?> cls) {
        return cls != null && isFreeBuilderGenerated(cls.getName());
    }

    private static boolean isFreeBuilderGenerated(final @Nullable String className) {
        return className != null && (className.contains("_Builder$") || className.endsWith("_Builder"));
    }
}
