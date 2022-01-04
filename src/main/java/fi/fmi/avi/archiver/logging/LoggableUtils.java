package fi.fmi.avi.archiver.logging;

import static java.util.Objects.requireNonNull;

import java.util.BitSet;
import java.util.stream.IntStream;

import com.google.common.base.Preconditions;

public final class LoggableUtils {
    private static final BitSet FORBIDDEN_CHARS = charsToBitSet("():@");
    private static final BitSet VALID_CHAR_TYPES = IntStream.of(//
                    Character.UPPERCASE_LETTER, Character.LOWERCASE_LETTER, Character.TITLECASE_LETTER, Character.MODIFIER_LETTER, Character.OTHER_LETTER, //
                    Character.DECIMAL_DIGIT_NUMBER, //
                    Character.CONNECTOR_PUNCTUATION, Character.DASH_PUNCTUATION, Character.OTHER_PUNCTUATION, //
                    Character.MATH_SYMBOL)//
            .collect(BitSet::new, BitSet::set, BitSet::or);

    private static final char HORIZONTAL_ELLIPSIS = '.'; // Not using '\u2026', as Logback outputs it as '?'.
    private static final int HORIZONTAL_ELLIPSIS_COUNT = 3;

    private LoggableUtils() {
        throw new AssertionError();
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
        Preconditions.checkState(maxLength >= 0, "maxLength must be non-negative");
        Preconditions.checkState(startIndex >= 0, "startIndex must be non-negative");
        Preconditions.checkState(endIndex >= 0, "endIndex must be non-negative");
        Preconditions.checkState(startIndex <= endIndex, "endIndex cannot be before startIndex");
        if (maxLength == 0) {
            return "";
        }
        final StringBuilder builder = new StringBuilder(maxLength);
        final int sanitizedStartIndex = skipWhitespace(content, startIndex, endIndex);
        final int sanitizedEndIndex = appendSanitizedContent(builder, content, maxLength, sanitizedStartIndex, endIndex);
        if (isHorizontalEllipsisNeeded(builder, sanitizedEndIndex, endIndex)) {
            writeHorizontalEllipsis(builder);
        }
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
                if (builder.charAt(builder.length() - 1) != ' ') {
                    builder.append(' ');
                }
            } else if (VALID_CHAR_TYPES.get(Character.getType(ch)) && !FORBIDDEN_CHARS.get(ch)) {
                builder.append(ch);
            } else if (!Character.isISOControl(ch)) {
                builder.append('?');
            }
            i++;
        }
        return i;
    }

    private static boolean isHorizontalEllipsisNeeded(final StringBuilder builder, final int sanitizedEndIndex, final int contentEndIndex) {
        return sanitizedEndIndex < contentEndIndex && builder.length() > HORIZONTAL_ELLIPSIS_COUNT * 2;
    }

    private static void writeHorizontalEllipsis(final StringBuilder builder) {
        for (int length = builder.length(), i = length - HORIZONTAL_ELLIPSIS_COUNT; i < length; i++) {
            builder.setCharAt(i, HORIZONTAL_ELLIPSIS);
        }
    }
}
