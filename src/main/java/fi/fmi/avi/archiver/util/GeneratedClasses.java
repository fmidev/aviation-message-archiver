package fi.fmi.avi.archiver.util;

import javax.annotation.Nullable;

public final class GeneratedClasses {
    private GeneratedClasses() {
        throw new AssertionError();
    }

    public static boolean isKnownGenerated(@Nullable final Class<?> cls) {
        return cls != null && (isKnownGeneratedByName(cls.getName()) || isNativeGenerated(cls));
    }

    private static boolean isKnownGeneratedByName(@Nullable final String className) {
        return isAutoValueGenerated(className) || isFreeBuilderGenerated(className);
    }

    public static boolean isNativeGenerated(@Nullable final Class<?> cls) {
        return cls != null && cls.isRecord();
    }

    public static boolean isAutoValueGenerated(@Nullable final Class<?> cls) {
        return cls != null && isAutoValueGenerated(cls.getName());
    }

    private static boolean isAutoValueGenerated(@Nullable final String className) {
        return className != null && className.substring(className.lastIndexOf('.') + 1).startsWith("AutoValue_");
    }

    public static boolean isFreeBuilderGenerated(@Nullable final Class<?> cls) {
        return cls != null && isFreeBuilderGenerated(cls.getName());
    }

    private static boolean isFreeBuilderGenerated(@Nullable final String className) {
        return className != null && (className.contains("_Builder$") || className.endsWith("_Builder"));
    }
}
