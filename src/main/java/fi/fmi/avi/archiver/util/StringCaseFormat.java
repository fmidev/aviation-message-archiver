package fi.fmi.avi.archiver.util;

import static java.util.Objects.requireNonNull;

public final class StringCaseFormat {
    private StringCaseFormat() {
        throw new AssertionError();
    }

    /**
     * Transforms provided dash-separated {@code input} string to camel case string.
     * It removes all dashes and transforms any character following a dash to upper case. Case of any other character is retained as is.
     *
     * @param input
     *         input dash-separated string
     *
     * @return provided string in camel case
     */
    public static String dashToCamel(final String input) {
        requireNonNull(input, "input");
        final int originalLength = input.length();
        final StringBuilder builder = new StringBuilder(originalLength);
        boolean toUpperCase = false;
        boolean changed = false;
        for (int i = 0; i < originalLength; i++) {
            char currentChar = input.charAt(i);
            if (currentChar == '-') {
                toUpperCase = true;
                changed = true;
                continue;
            }
            if (toUpperCase) {
                currentChar = Character.toUpperCase(currentChar);
                toUpperCase = false;
            }
            builder.append(currentChar);
        }
        return changed ? builder.toString() : input;
    }
}
