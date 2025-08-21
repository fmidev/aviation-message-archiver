package fi.fmi.avi.archiver.util.instantiation;

import java.util.Arrays;

import static java.util.Objects.requireNonNull;

public enum BeanMethodNamePrefix {
    GET, IS, SET;

    private final String prefix;

    BeanMethodNamePrefix() {
        prefix = name().toLowerCase();
    }

    public static String stripAny(final String methodName) {
        return Arrays.stream(values())
                .filter(prefix -> prefix.isPrefixed(methodName))
                .findAny()
                .map(prefix -> prefix.strip(methodName))
                .orElse(methodName);
    }

    public String getPrefix() {
        return prefix;
    }

    public boolean isPrefixed(final String methodName) {
        requireNonNull(methodName, "methodName");
        return methodName.length() > prefix.length()
                && methodName.startsWith(prefix)
                && Character.isUpperCase(methodName.charAt(prefix.length()));
    }

    public String prefix(final String propertyName) {
        requireNonNull(propertyName, "propertyName");
        if (isPrefixed(propertyName) || propertyName.isEmpty()) {
            return propertyName;
        } else {
            return prefix + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
        }
    }

    public String strip(final String methodName) {
        requireNonNull(methodName, "methodName");
        if (isPrefixed(methodName)) {
            return Character.toLowerCase(methodName.charAt(prefix.length())) + methodName.substring(prefix.length() + 1);
        } else {
            return methodName;
        }
    }
}
