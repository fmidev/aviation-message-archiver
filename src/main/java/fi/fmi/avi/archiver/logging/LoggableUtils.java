package fi.fmi.avi.archiver.logging;

import static java.util.Objects.requireNonNull;

import java.util.BitSet;
import java.util.stream.IntStream;

import com.google.common.base.Preconditions;

public final class LoggableUtils {
    private static final BitSet VALID_CHAR_TYPES = intsToBitSet(//
            Character.UPPERCASE_LETTER, Character.LOWERCASE_LETTER, Character.OTHER_LETTER,//
            Character.DECIMAL_DIGIT_NUMBER, //
            Character.CONNECTOR_PUNCTUATION, Character.DASH_PUNCTUATION, Character.OTHER_PUNCTUATION, //
            Character.MATH_SYMBOL);
    private static final BitSet INVALID_CHAR_TYPES_TO_OMIT = intsToBitSet(Character.CONTROL);
    /**
     * Characters that act as separator of a (sanitized) content block shall be substituted.
     */
    private static final BitSet CUSTOM_INVALID_CHARS = charsToBitSet("()<>:");
    private static final char WHITESPACE_SUBSTITUTE = ' ';
    private static final char INVALID_CHAR_SUBSTITUTE = '?';
    private static final char HORIZONTAL_ELLIPSIS = '.'; // Not using '\u2026' ('…'), as Logback outputs it as '?'.
    private static final int HORIZONTAL_ELLIPSIS_COUNT = 3;

    private LoggableUtils() {
        throw new AssertionError();
    }

    private static BitSet intsToBitSet(final int... ints) {
        return IntStream.of(ints).collect(BitSet::new, BitSet::set, BitSet::or);
    }

    @SuppressWarnings("SameParameterValue")
    private static BitSet charsToBitSet(final String chars) {
        final BitSet builder = new BitSet();
        for (int i = 0, length = chars.length(); i < length; i++) {
            builder.set(chars.charAt(i));
        }
        return builder;
    }

    public static String sanitize(final String content, final int maxLength) {
        requireNonNull(content, "content");
        return sanitize(content, maxLength, 0, content.length());
    }

    public static String sanitize(final String content, final int maxLength, final int startIndex, final int endIndex) {
        requireNonNull(content, "content");
        Preconditions.checkArgument(maxLength >= 0, "maxLength must be non-negative");
        Preconditions.checkPositionIndexes(startIndex, endIndex, content.length());
        final StringBuilder builder = new StringBuilder(Math.min(endIndex - startIndex, maxLength));
        final int sanitizedStartIndex = skipWhitespace(content, startIndex, endIndex);
        final int sanitizedEndIndex = appendSanitizedContent(builder, content, maxLength, sanitizedStartIndex, endIndex);
        if (isHorizontalEllipsisNeeded(builder, sanitizedEndIndex, endIndex)) {
            writeHorizontalEllipsis(builder);
        }
        decreaseLengthOnTrailingWhitespace(builder, 0);
        return builder.toString();
    }

    private static int skipWhitespace(final String content, final int startIndex, final int endIndex) {
        int i = startIndex;
        while (i < endIndex && Character.isWhitespace(content.charAt(i))) {
            i++;
        }
        return i;
    }

    private static int appendSanitizedContent(final StringBuilder builder, final String content, final int maxLength, final int startIndex,
            final int endIndex) {
        int i = startIndex;
        while (i < endIndex && builder.length() < maxLength) {
            final char ch = content.charAt(i);
            if (Character.isWhitespace(ch)) {
                if (builder.charAt(builder.length() - 1) != WHITESPACE_SUBSTITUTE) {
                    builder.append(WHITESPACE_SUBSTITUTE);
                }
            } else if (VALID_CHAR_TYPES.get(Character.getType(ch)) && !CUSTOM_INVALID_CHARS.get(ch)) {
                builder.append(ch);
            } else if (!INVALID_CHAR_TYPES_TO_OMIT.get(Character.getType(ch))) {
                builder.append(INVALID_CHAR_SUBSTITUTE);
            }
            i++;
        }
        return i;
    }

    private static void decreaseLengthOnTrailingWhitespace(final StringBuilder builder, final int whitespaceIndexFromEnd) {
        final int decreasedLength = builder.length() - 1;
        if (decreasedLength >= whitespaceIndexFromEnd && builder.charAt(decreasedLength - whitespaceIndexFromEnd) == WHITESPACE_SUBSTITUTE) {
            builder.setLength(decreasedLength);
        }
    }

    private static boolean isHorizontalEllipsisNeeded(final StringBuilder builder, final int sanitizedEndIndex, final int contentEndIndex) {
        return sanitizedEndIndex < contentEndIndex && builder.length() > HORIZONTAL_ELLIPSIS_COUNT * 2;
    }

    private static void writeHorizontalEllipsis(final StringBuilder builder) {
        decreaseLengthOnTrailingWhitespace(builder, HORIZONTAL_ELLIPSIS_COUNT);
        for (int length = builder.length(), i = length - HORIZONTAL_ELLIPSIS_COUNT; i < length; i++) {
            builder.setCharAt(i, HORIZONTAL_ELLIPSIS);
        }
    }
}
