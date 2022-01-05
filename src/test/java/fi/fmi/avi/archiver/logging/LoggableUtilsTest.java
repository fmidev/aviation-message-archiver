package fi.fmi.avi.archiver.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

class LoggableUtilsTest {

    private static final String VALID_CONTENT = "Lu ABCZÄÖ\u03A3" //
            + " Ll defzäö\u026E" //
            + " Lo \u00AA\u05DE\u076B" //
            + " Nd 0189\u1B50" //
            + " Pc _\u203F\uFF3F" //
            + " Pd -\u2011\uFF0D" //
            + " Po !\"#%&'*/,.\\?@\u2CFF" //
            + " Sm +×÷=\u22C2";

    private static void assertSanitize(final String content, final String expected) {
        assertSanitize(content, Integer.MAX_VALUE, expected);
    }

    private static void assertSanitize(final String content, final int maxLength, final String expected) {
        assertSanitizationExpectationValidity(expected, maxLength);
        assertThat(LoggableUtils.sanitize(content, maxLength))//
                .as("sanitized")//
                .isEqualTo(expected);
    }

    private static void assertSanitizationExpectationValidity(final String expected, final int maxLength) {
        assertThat(expected)//
                .as("length of expected must be less or equal to maxLength for test to be valid")//
                .hasSizeLessThanOrEqualTo(maxLength);
    }

    private static void assertSanitize(final String content, final int maxLength, final int startIndex, final int endIndex, final String expected) {
        assertSanitizationExpectationValidity(expected, maxLength);
        assertThat(LoggableUtils.sanitize(content, maxLength, startIndex, endIndex))//
                .as("sanitized")//
                .isEqualTo(expected);
    }

    private static IntStream controlChars() {
        return IntStream.concat(//
                IntStream.rangeClosed('\u0000', '\u001F'), //
                IntStream.rangeClosed('\u007F', '\u009F'));
    }

    @Test
    void sanitize_returns_valid_string_as_is() {
        assertSanitize(VALID_CONTENT, VALID_CONTENT);
    }

    @Test
    void sanitize_returns_valid_string_as_is_when_maxLength_is_equal_to_content_length() {
        assertSanitize(VALID_CONTENT, VALID_CONTENT.length(), VALID_CONTENT);
    }

    @Test
    void sanitize_enforces_max_length() {
        assertSanitize("abcdefghijklmnop", 3, "abc");
    }

    @Test
    void sanitize_accepts_zero_max_length() {
        assertSanitize("a", 0, "");
    }

    @Test
    void sanitize_fails_on_negative_max_length() {
        assertThatIllegalArgumentException().isThrownBy(() -> LoggableUtils.sanitize("a", -1))//
                .withMessageContaining("maxLength");
    }

    @Test
    void sanitize_trims_start_of_provided_string() {
        assertSanitize("  \n\tstring", "string");
    }

    @Test
    void sanitize_trims_end_of_provided_string() {
        assertSanitize("string\n  \t  ", "string");
    }

    @Test
    void sanitize_returns_string_containing_only_whitespaces_as_empty_string1() {
        assertSanitize(" ", "");
    }

    @Test
    void sanitize_returns_string_containing_only_whitespaces_as_empty_string2() {
        assertSanitize("\n  \t   \n", "");
    }

    @Test
    void sanitize_collapses_multiple_whitespaces_into_single_space_char() {
        assertSanitize(" a  b\tc\nd\n\t e\n", "a b c d e");
    }

    @Test
    void sanitize_substitutes_chars_of_invalid_type() {
        assertSanitize("abc$[]^»¼½¾\u102b\u102c\u102d\u102e\ufffc\ufffd\ufffe\uffffdef", "abc????????????????def");
    }

    @Test
    void sanitize_substitutes_explicitly_defined_invalid_chars() {
        assertSanitize("abc()<>:def", "abc?????def");
    }

    @Test
    void sanitize_omits_control_chars() {
        final String controlChars = controlChars()//
                .filter(codePoint -> !Character.isWhitespace(codePoint) && !Character.isLetterOrDigit(codePoint))//
                .collect(StringBuilder::new, (builder, codePoint) -> builder.append((char) codePoint), StringBuilder::append)//
                .toString();
        assertSanitize("abc" + controlChars + "def", "abcdef");
    }

    @Test
    void sanitize_treats_whitespace_control_chars_as_whitespaces() {
        final String whitespaceControlChars = controlChars()//
                .filter(Character::isWhitespace)//
                .collect(StringBuilder::new, (builder, codePoint) -> builder.append((char) codePoint), StringBuilder::append)//
                .toString();
        assertSanitize("abc" + whitespaceControlChars + "def", "abc def");
    }

    @Test
    void sanitize_does_not_replace_end_with_horizontal_ellipsis_when_string_is_short_enough() {
        assertSanitize("abcdefghijklmnop", 6, "abcdef");
    }

    @Test
    void sanitize_replaces_end_with_horizontal_ellipsis_when_string_is_long_enough() {
        assertSanitize("abcdefghijklmnop", 7, "abcd...");
    }

    @Test
    void sanitize_omits_spaces_prepending_horizontal_ellipsis() {
        assertSanitize("abc  def  ghi  jkl", 11, "abc def...");
    }

    @Test
    void sanitize_returns_string_of_maximum_possible_length() {
        assertSanitize("abc  def  ghi  jkl", 12, "abc def g...");
    }

    @Test
    void sanitize_substring() {
        assertSanitize("\n <X> Abc   \t 1234(5)6789 \n Def \t <y>\t ", 18, 5, 32, "Abc 1234?5?6789...");
    }

    @Test
    void sanitize_complete_content_as_substring() {
        final String content = "\n <X> Abc   \t 1234(5)6789 \n Def \t <y>\t ";
        assertSanitize(content, 27, 0, content.length(), "?X? Abc 1234?5?6789 Def...");
    }

    @Test
    void sanitize_substring_fails_when_startIndex_is_negative() {
        assertThatExceptionOfType(IndexOutOfBoundsException.class)//
                .isThrownBy(() -> LoggableUtils.sanitize("xx", 2, -1, 2))//
                .withMessageContaining("start index")//
                .withMessageContaining("negative");
    }

    @Test
    void sanitize_substring_fails_when_startIndex_is_greater_than_content_length() {
        assertThatExceptionOfType(IndexOutOfBoundsException.class)//
                .isThrownBy(() -> LoggableUtils.sanitize("xx", 2, 3, 3))//
                .withMessageContaining("start index")//
                .withMessageContaining("size");
    }

    @Test
    void sanitize_substring_fails_when_startIndex_is_greater_than_end_index() {
        assertThatExceptionOfType(IndexOutOfBoundsException.class)//
                .isThrownBy(() -> LoggableUtils.sanitize("xx", 2, 1, 0))//
                .withMessageContaining("start index")//
                .withMessageContaining("end index");
    }

    @Test
    void sanitize_substring_fails_when_endIndex_is_greater_than_content_length() {
        assertThatExceptionOfType(IndexOutOfBoundsException.class)//
                .isThrownBy(() -> LoggableUtils.sanitize("xx", 2, 0, 3))//
                .withMessageContaining("end index")//
                .withMessageContaining("size");
    }
}
