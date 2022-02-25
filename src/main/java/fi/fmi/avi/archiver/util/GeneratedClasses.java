package fi.fmi.avi.archiver.util;

import javax.annotation.Nullable;

public final class GeneratedClasses {
    private GeneratedClasses() {
        throw new AssertionError();
    }

    public static boolean isKnownGeneratedClass(@Nullable final Class<?> cls) {
        return cls != null && isKnownGeneratedClass(cls.getName());
    }

    public static boolean isKnownGeneratedClass(@Nullable final String className) {
        return isAutoValueClass(className) || isFreeBuilderClass(className);
    }

    public static boolean isAutoValueClass(@Nullable final Class<?> cls) {
        return cls != null && isAutoValueClass(cls.getName());
    }

    public static boolean isAutoValueClass(@Nullable final String className) {
        return className != null && className.substring(className.lastIndexOf('.') + 1).startsWith("AutoValue_");
    }

    public static boolean isFreeBuilderClass(@Nullable final Class<?> cls) {
        return cls != null && isFreeBuilderClass(cls.getName());
    }

    public static boolean isFreeBuilderClass(@Nullable final String className) {
        return className != null && (className.contains("_Builder$") || className.endsWith("_Builder"));
    }
}
