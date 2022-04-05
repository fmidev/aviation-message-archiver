package fi.fmi.avi.archiver.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EmptySource;

class StringCaseFormatTest {
    @ParameterizedTest
    @EmptySource
    @CsvSource({//
            "lowerCamelString", //
            "UpperCamelString", //
            "lower_underscore_string", //
            "UPPER_UNDERSCORE_STRING", //
            "mixed_UNDERSCORE_sTrInG", //
            "string_with_123_numbers", //
            "string_with_.:!#_special_characters", //
            "mixedString_containing_DIFFERENT_987_,.:;!styles", //
    })
    void dashToCamel_given_string_without_dashes_returns_input(final String input) {
        assertThat(StringCaseFormat.dashToCamel(input)).isSameAs(input);
    }

    @ParameterizedTest
    @CsvSource({//
            "simple-string, simpleString", //
            "numbered-123-string, numbered123String", //
            "string-with-123numbers, stringWith123numbers", //
            "All-Words-Capitalized, AllWordsCapitalized", //
            "ALL-WORDS-UPPER, ALLWORDSUPPER", //
            "mIxEd-CaSe-wOrDs, mIxEdCaSeWOrDs", //
            "some-l-e-t-t-E-R-s-1-2-3-.-:-;-etc, someLETTERS123.:;Etc", //
            "several--dashes---between----words, severalDashesBetweenWords", //
            "number-in-the-end7, numberInTheEnd7", //
            "number-in-the-end-8, numberInTheEnd8", //
            "1starts-with-number, 1startsWithNumber", //
            "2-starts-with-number, 2StartsWithNumber", //
    })
    void dashToCamel_removes_dashes_and_transforms_following_char_to_upper_case(final String input, final String expected) {
        assertThat(StringCaseFormat.dashToCamel(input)).isEqualTo(expected);
    }
}
