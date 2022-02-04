package fi.fmi.avi.archiver.message.populator.conditional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class GeneralPropertyPredicateTest {
    private static final String TEST_STRING1 = "testString1";
    private static final String TEST_STRING2 = "testString2";
    private static final String TEST_STRING3 = "testString3";
    private static final String TEST_STRING4 = "testString4";
    private static final String TEST_STRING5 = "testString5";
    private static final Pattern TEST_PATTERN = Pattern.compile("testPattern");

    @Test
    void getIsAnyOf_returns_empty_set_by_default() {
        final GeneralPropertyPredicate<String> predicate = GeneralPropertyPredicate.<String> builder().build();

        final Set<String> result = predicate.getIsAnyOf();

        assertThat(result).isEmpty();
    }

    @Test
    void getIsNoneOf_returns_empty_set_by_default() {
        final GeneralPropertyPredicate<String> predicate = GeneralPropertyPredicate.<String> builder().build();

        final Set<String> result = predicate.getIsNoneOf();

        assertThat(result).isEmpty();
    }

    @Test
    void getMatches_returns_empty_pattern_by_default() {
        final GeneralPropertyPredicate<String> predicate = GeneralPropertyPredicate.<String> builder().build();

        final Pattern result = predicate.getMatches();

        assertThat(result.pattern()).isEmpty();
    }

    @Test
    void getDoesNotMatch_returns_empty_pattern_by_default() {
        final GeneralPropertyPredicate<String> predicate = GeneralPropertyPredicate.<String> builder().build();

        final Pattern result = predicate.getDoesNotMatch();

        assertThat(result.pattern()).isEmpty();
    }

    @Test
    void getIsAbsent_returns_false_by_default() {
        final GeneralPropertyPredicate<String> predicate = GeneralPropertyPredicate.<String> builder().build();

        final boolean result = predicate.getIsAbsent();

        assertThat(result).isFalse();
    }

    @Test
    void test_returns_true_by_default() {
        final GeneralPropertyPredicate<String> predicate = GeneralPropertyPredicate.<String> builder().build();

        final boolean result = predicate.test(TEST_STRING1);

        assertThat(result).isTrue();
    }

    @Test
    void test_given_null_returns_true_when_isAbsent_is_true() {
        final GeneralPropertyPredicate<String> predicate = GeneralPropertyPredicate.<String> builder()//
                .setIsAbsent(true)//
                .build();

        final boolean result = predicate.test(null);

        assertThat(result).isTrue();
    }

    @Test
    void test_given_empty_Optional_returns_true_when_isAbsent_is_true() {
        final GeneralPropertyPredicate<Optional<String>> predicate = GeneralPropertyPredicate.<Optional<String>> builder()//
                .setIsAbsent(true)//
                .build();

        final boolean result = predicate.test(Optional.empty());

        assertThat(result).isTrue();
    }

    @Test
    void test_given_an_object_returns_false_when_isAbsent_is_true() {
        final GeneralPropertyPredicate<String> predicate = GeneralPropertyPredicate.<String> builder()//
                .setIsAbsent(true)//
                .build();

        final boolean result = predicate.test(TEST_STRING1);

        assertThat(result).isFalse();
    }

    @Test
    void test_given_Optional_with_value_returns_false_when_isAbsent_is_true() {
        final GeneralPropertyPredicate<Optional<String>> predicate = GeneralPropertyPredicate.<Optional<String>> builder()//
                .setIsAbsent(true)//
                .build();

        final boolean result = predicate.test(Optional.of(TEST_STRING1));

        assertThat(result).isFalse();
    }

    @Test
    void test_given_value_in_isAnyOf_returns_true() {
        final GeneralPropertyPredicate<String> predicate = GeneralPropertyPredicate.<String> builder()//
                .addIsAnyOf(TEST_STRING1, TEST_STRING2, TEST_STRING3)//
                .build();

        final boolean result = predicate.test(TEST_STRING2);

        assertThat(result).isTrue();
    }

    @Test
    void test_given_value_in_isNoneOf_returns_false() {
        final GeneralPropertyPredicate<String> predicate = GeneralPropertyPredicate.<String> builder()//
                .addIsNoneOf(TEST_STRING1, TEST_STRING2, TEST_STRING3)//
                .build();

        final boolean result = predicate.test(TEST_STRING2);

        assertThat(result).isFalse();
    }

    @Test
    void test_given_value_not_in_isNoneOf_returns_true() {
        final GeneralPropertyPredicate<String> predicate = GeneralPropertyPredicate.<String> builder()//
                .addIsNoneOf(TEST_STRING1, TEST_STRING2, TEST_STRING3)//
                .build();

        final boolean result = predicate.test(TEST_STRING4);

        assertThat(result).isTrue();
    }

    @Test
    void test_given_value_matching_matches_returns_true() {
        final GeneralPropertyPredicate<String> predicate = GeneralPropertyPredicate.<String> builder()//
                .setMatches(Pattern.compile("^.*String2$"))//
                .build();

        final boolean result = predicate.test(TEST_STRING2);

        assertThat(result).isTrue();
    }

    /**
     * Empty pattern is considered nonexistent, and therefore anything passes.
     */
    @Test
    void test_given_any_value_matches_empty_pattern_returns_true() {
        final GeneralPropertyPredicate<String> predicate = GeneralPropertyPredicate.<String> builder()//
                .setMatches(Pattern.compile(""))//
                .build();

        final boolean result = predicate.test("any string");

        assertThat(result).isTrue();
    }

    @Test
    void test_given_value_not_matching_matches_returns_true() {
        final GeneralPropertyPredicate<String> predicate = GeneralPropertyPredicate.<String> builder()//
                .setMatches(Pattern.compile("^.*String2$"))//
                .build();

        final boolean result = predicate.test(TEST_STRING1);

        assertThat(result).isFalse();
    }

    /**
     * Empty pattern is considered nonexistent, and therefore anything passes.
     */
    @Test
    void test_given_null_matches_empty_pattern_returns_true() {
        final GeneralPropertyPredicate<String> predicate = GeneralPropertyPredicate.<String> builder()//
                .setMatches(Pattern.compile(""))//
                .build();

        final boolean result = predicate.test(null);

        assertThat(result).isTrue();
    }

    @Test
    void test_given_null_matches_empty_string_pattern_returns_true() {
        final GeneralPropertyPredicate<String> predicate = GeneralPropertyPredicate.<String> builder()//
                .setMatches(Pattern.compile("^$"))//
                .build();

        final boolean result = predicate.test(null);

        assertThat(result).isTrue();
    }

    /**
     * Empty pattern is considered nonexistent, and therefore anything passes.
     */
    @Test
    void test_given_null_doesNotMatch_empty_pattern_returns_true() {
        final GeneralPropertyPredicate<String> predicate = GeneralPropertyPredicate.<String> builder()//
                .setDoesNotMatch(Pattern.compile(""))//
                .build();

        final boolean result = predicate.test(null);

        assertThat(result).isTrue();
    }

    @Test
    void test_given_null_doesNotMatch_empty_string_pattern_returns_true() {
        final GeneralPropertyPredicate<String> predicate = GeneralPropertyPredicate.<String> builder()//
                .setDoesNotMatch(Pattern.compile("^$"))//
                .build();

        final boolean result = predicate.test(null);

        assertThat(result).isFalse();
    }

    @Test
    void test_given_value_matching_doesNotMatch_returns_false() {
        final GeneralPropertyPredicate<String> predicate = GeneralPropertyPredicate.<String> builder()//
                .setDoesNotMatch(Pattern.compile("^.*String2$"))//
                .build();

        final boolean result = predicate.test(TEST_STRING2);

        assertThat(result).isFalse();
    }

    @Test
    void test_given_value_not_matching_setDoesNotMatch_returns_true() {
        final GeneralPropertyPredicate<String> predicate = GeneralPropertyPredicate.<String> builder()//
                .setDoesNotMatch(Pattern.compile("^.*String2$"))//
                .build();

        final boolean result = predicate.test(TEST_STRING1);

        assertThat(result).isTrue();
    }

    /**
     * Empty pattern is considered nonexistent, and therefore anything passes.
     */
    @Test
    void test_given_any_value_setDoesNotMatch_empty_pattern_returns_true() {
        final GeneralPropertyPredicate<String> predicate = GeneralPropertyPredicate.<String> builder()//
                .setDoesNotMatch(Pattern.compile(""))//
                .build();

        final boolean result = predicate.test("any string");

        assertThat(result).isTrue();
    }

    @Test
    void test_given_value_satisfying_all_conditions_returns_true() {
        final GeneralPropertyPredicate<String> predicate = GeneralPropertyPredicate.<String> builder()//
                .addIsAnyOf(TEST_STRING1, TEST_STRING2, TEST_STRING4, TEST_STRING5)//
                .addIsNoneOf(TEST_STRING4)//
                .setMatches(Pattern.compile("^.*String[1-4]$"))//
                .setDoesNotMatch(Pattern.compile("^.*String2$"))//
                .build();

        final boolean result = predicate.test(TEST_STRING1);

        assertThat(result).isTrue();
    }

    @ParameterizedTest
    @CsvSource({ "testString2", "testString3", "testString4", "testString5" })
    void test_given_value_not_satisfying_all_conditions_returns_false(final String input) {
        final GeneralPropertyPredicate<String> predicate = GeneralPropertyPredicate.<String> builder()//
                .addIsAnyOf(TEST_STRING1, TEST_STRING2, TEST_STRING4, TEST_STRING5)//
                .addIsNoneOf(TEST_STRING4)//
                .setMatches(Pattern.compile("^.*String[1-4]$"))//
                .setDoesNotMatch(Pattern.compile("^.*String2$"))//
                .build();

        final boolean result = predicate.test(input);

        assertThat(result).isFalse();
    }

    @Test
    void builder_build_throws_exception_when_absent_is_true_and_isAnyOf_has_values() {
        final GeneralPropertyPredicate.Builder<?> builder = GeneralPropertyPredicate.builder()//
                .setIsAbsent(true)//
                .addIsAnyOf(TEST_STRING1);

        assertThatIllegalStateException().isThrownBy(builder::build)//
                .withMessageContaining("isAbsent");
    }

    @Test
    void builder_build_throws_exception_when_absent_is_true_and_isNoneOf_has_values() {
        final GeneralPropertyPredicate.Builder<?> builder = GeneralPropertyPredicate.builder()//
                .setIsAbsent(true)//
                .addIsNoneOf(TEST_STRING1);

        assertThatIllegalStateException().isThrownBy(builder::build)//
                .withMessageContaining("isAbsent");
    }

    @Test
    void builder_build_throws_exception_when_absent_is_true_and_matches_is_non_empty_pattern() {
        final GeneralPropertyPredicate.Builder<?> builder = GeneralPropertyPredicate.builder()//
                .setIsAbsent(true)//
                .setMatches(TEST_PATTERN);

        assertThatIllegalStateException().isThrownBy(builder::build)//
                .withMessageContaining("isAbsent");
    }

    @Test
    void builder_build_throws_exception_when_absent_is_true_and_doesNotMatch_is_non_empty_pattern() {
        final GeneralPropertyPredicate.Builder<?> builder = GeneralPropertyPredicate.builder()//
                .setIsAbsent(true)//
                .setDoesNotMatch(TEST_PATTERN);

        assertThatIllegalStateException().isThrownBy(builder::build)//
                .withMessageContaining("isAbsent");
    }

    @Test
    void builder_setIsAnyOf_replaces_existing_values_with_given_values() {
        final GeneralPropertyPredicate.Builder<String> builder = GeneralPropertyPredicate.<String> builder()//
                .addIsAnyOf(TEST_STRING1, TEST_STRING2);
        final HashSet<String> replacement = new HashSet<>(Arrays.asList(TEST_STRING2, TEST_STRING3));

        final Set<String> result = builder.setIsAnyOf(replacement)//
                .getIsAnyOf();

        assertThat(result).containsExactlyInAnyOrderElementsOf(replacement);
    }

    @Test
    void builder_setIsNoneOf_replaces_existing_values_with_given_values() {
        final GeneralPropertyPredicate.Builder<String> builder = GeneralPropertyPredicate.<String> builder()//
                .addIsNoneOf(TEST_STRING1, TEST_STRING2);
        final HashSet<String> replacement = new HashSet<>(Arrays.asList(TEST_STRING2, TEST_STRING3));

        final Set<String> result = builder.setIsNoneOf(replacement)//
                .getIsNoneOf();

        assertThat(result).containsExactlyInAnyOrderElementsOf(replacement);
    }

    @Test
    void builder_transform_returns_new_builder_with_values_in_isAnyOf_transformed() {
        final GeneralPropertyPredicate.Builder<Integer> builder = GeneralPropertyPredicate.<Integer> builder()//
                .addIsAnyOf(1, 2);

        final GeneralPropertyPredicate.Builder<String> result = builder.transform(Object::toString);

        //noinspection AssertBetweenInconvertibleTypes
        assertThat(result).isNotSameAs(builder);
        assertThat(result.getIsAnyOf()).containsExactlyInAnyOrder("1", "2");
    }

    @Test
    void builder_transform_returns_new_builder_with_values_in_isNoneOf_transformed() {
        final GeneralPropertyPredicate.Builder<Integer> builder = GeneralPropertyPredicate.<Integer> builder()//
                .addIsNoneOf(1, 2);

        final GeneralPropertyPredicate.Builder<String> result = builder.transform(Object::toString);

        //noinspection AssertBetweenInconvertibleTypes
        assertThat(result).isNotSameAs(builder);
        assertThat(result.getIsNoneOf()).containsExactlyInAnyOrder("1", "2");
    }

    @Test
    void builder_transform_returns_new_builder_with_matches_as_is() {
        final Pattern pattern = TEST_PATTERN;
        final GeneralPropertyPredicate.Builder<?> builder = GeneralPropertyPredicate.builder()//
                .setMatches(pattern);

        final GeneralPropertyPredicate.Builder<String> result = builder.transform(Object::toString);

        assertThat(result).isNotSameAs(builder);
        assertThat(result.getMatches()).isEqualTo(pattern);
    }

    @Test
    void builder_transform_returns_new_builder_with_doesNotMatch_as_is() {
        final Pattern pattern = TEST_PATTERN;
        final GeneralPropertyPredicate.Builder<?> builder = GeneralPropertyPredicate.builder()//
                .setDoesNotMatch(pattern);

        final GeneralPropertyPredicate.Builder<String> result = builder.transform(Object::toString);

        assertThat(result).isNotSameAs(builder);
        assertThat(result.getDoesNotMatch()).isEqualTo(pattern);
    }

    @ParameterizedTest
    @CsvSource({ "true", "false" })
    void builder_transform_returns_new_builder_with_isAbsent_as_is(final boolean absent) {
        final GeneralPropertyPredicate.Builder<?> builder = GeneralPropertyPredicate.builder()//
                .setIsAbsent(absent);

        final GeneralPropertyPredicate.Builder<String> result = builder.transform(Object::toString);

        assertThat(result).isNotSameAs(builder);
        assertThat(result.getIsAbsent()).isEqualTo(absent);
    }

    @Test
    void validate_passes_when_all_elements_of_isAnyOf_satisfy_validator() {
        final GeneralPropertyPredicate.Builder<String> builder = GeneralPropertyPredicate.<String> builder()//
                .addIsAnyOf(TEST_STRING1, TEST_STRING2);
        final Collection<String> validValues = Arrays.asList(TEST_STRING1, TEST_STRING2, TEST_STRING3);

        builder.validate(validValues::contains);
    }

    @Test
    void validate_throws_exception_when_not_all_elements_of_isAnyOf_satisfy_validator() {
        final GeneralPropertyPredicate.Builder<String> builder = GeneralPropertyPredicate.<String> builder()//
                .addIsAnyOf(TEST_STRING1, TEST_STRING2, TEST_STRING3);
        final Collection<String> validValues = Arrays.asList(TEST_STRING1, TEST_STRING3, TEST_STRING4);

        assertThatIllegalStateException()//
                .isThrownBy(() -> builder.validate(validValues::contains))//
                .withMessageContaining("isAnyOf")//
                .withMessageContaining(TEST_STRING2);
    }

    @Test
    void validate_passes_when_all_elements_of_isNoneOf_satisfy_validator() {
        final GeneralPropertyPredicate.Builder<String> builder = GeneralPropertyPredicate.<String> builder()//
                .addIsNoneOf(TEST_STRING1, TEST_STRING2);
        final Collection<String> validValues = Arrays.asList(TEST_STRING1, TEST_STRING2, TEST_STRING3);

        builder.validate(validValues::contains);
    }

    @Test
    void validate_throws_exception_when_not_all_elements_of_isNoneOf_satisfy_validator() {
        final GeneralPropertyPredicate.Builder<String> builder = GeneralPropertyPredicate.<String> builder()//
                .addIsNoneOf(TEST_STRING1, TEST_STRING2, TEST_STRING3);
        final Collection<String> validValues = Arrays.asList(TEST_STRING1, TEST_STRING3, TEST_STRING4);

        assertThatIllegalStateException()//
                .isThrownBy(() -> builder.validate(validValues::contains))//
                .withMessageContaining("isNoneOf")//
                .withMessageContaining(TEST_STRING2);
    }

    @Test
    void validate_passes_regardless_of_matches_and_doesNotMatch() {
        final GeneralPropertyPredicate.Builder<String> builder = GeneralPropertyPredicate.<String> builder()//
                .setMatches(Pattern.compile("matchPattern"))//
                .setDoesNotMatch(Pattern.compile("doesNotMatchPattern"));
        final Collection<String> validValues = Collections.singleton(TEST_STRING1);

        builder.validate(validValues::contains);
    }

    @Test
    void validate_throws_exception_regardless_of_matches_and_doesNotMatch() {
        final GeneralPropertyPredicate.Builder<String> builder = GeneralPropertyPredicate.<String> builder()//
                .addIsAnyOf(TEST_STRING2)//
                .setMatches(Pattern.compile("matchPattern"))//
                .setDoesNotMatch(Pattern.compile("doesNotMatchPattern"));
        final Collection<String> validValues = Collections.singleton(TEST_STRING1);

        assertThatIllegalStateException()//
                .isThrownBy(() -> builder.validate(validValues::contains))//
                .withMessageContaining("isAnyOf")//
                .withMessageContaining(TEST_STRING2);
    }

    @Test
    void validate_passes_regardless_of_isAbsent_being_true() {
        final GeneralPropertyPredicate.Builder<String> builder = GeneralPropertyPredicate.<String> builder()//
                .setIsAbsent(true);
        final Collection<String> validValues = Arrays.asList(TEST_STRING1, TEST_STRING2, TEST_STRING3);

        builder.validate(validValues::contains);
    }

    @Test
    void validate_throws_exception_regardless_of_isAbsent_being_true() {
        final GeneralPropertyPredicate.Builder<String> builder = GeneralPropertyPredicate.<String> builder()//
                .addIsAnyOf(TEST_STRING2)//
                .setIsAbsent(true);
        final Collection<String> validValues = Collections.singleton(TEST_STRING1);

        assertThatIllegalStateException()//
                .isThrownBy(() -> builder.validate(validValues::contains))//
                .withMessageContaining("isAnyOf")//
                .withMessageContaining(TEST_STRING2);
    }

    @Test
    void toString_returns_initially_constant_string() {
        final String result = GeneralPropertyPredicate.builder()//
                .build()//
                .toString();
        assertThat(result).isEqualTo("isAnything");
    }

    @Test
    void toString_returns_constant_string_on_absent() {
        final String result = GeneralPropertyPredicate.builder()//
                .setIsAbsent(true)//
                .build()//
                .toString();
        assertThat(result).isEqualTo("isAbsent");
    }

    @Test
    void toString_returns_string_describing_only_condition() {
        final String result = GeneralPropertyPredicate.<Integer> builder()//
                .addIsNoneOf(4, 5, 6)//
                .build()//
                .toString();
        assertThat(result).isEqualTo("isNoneOf{[4, 5, 6]}");
    }

    @Test
    void toString_returns_string_describing_two_conditions() {
        final String result = GeneralPropertyPredicate.<Integer> builder()//
                .addIsNoneOf(4, 5, 6)//
                .setMatches(Pattern.compile("^\\d$"))//
                .build()//
                .toString();
        assertThat(result).isEqualTo("isNoneOf{[4, 5, 6]} & matches{^\\d$}");
    }

    @Test
    void toString_returns_string_describing_all_conditions() {
        final String result = GeneralPropertyPredicate.<Integer> builder()//
                .addIsAnyOf(1, 2, 3)//
                .addIsNoneOf(4, 5, 6)//
                .setMatches(Pattern.compile("^\\d$"))//
                .setDoesNotMatch(Pattern.compile("^[a-z]*$"))//
                .build()//
                .toString();
        assertThat(result).isEqualTo("isAnyOf{[1, 2, 3]} & isNoneOf{[4, 5, 6]} & matches{^\\d$} & doesNotMatch{^[a-z]*$}");
    }
}
