package fi.fmi.avi.archiver.message.processor.conditional;

import fi.fmi.avi.archiver.message.processor.conditional.GeneralPropertyPredicate.PresencePolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.*;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class GeneralPropertyPredicateTest {
    private static final String TEST_STRING1 = "testString1";
    private static final String TEST_STRING2 = "testString2";
    private static final String TEST_STRING3 = "testString3";
    private static final String TEST_STRING4 = "testString4";
    private static final String TEST_STRING5 = "testString5";
    private static final Pattern TEST_PATTERN = Pattern.compile("testPattern");

    @Test
    void getPresence_returns_PRESENT_by_default() {
        final GeneralPropertyPredicate<String> predicate = GeneralPropertyPredicate.<String>builder().build();

        final PresencePolicy result = predicate.getPresence();

        assertThat(result).isEqualTo(PresencePolicy.PRESENT);
    }

    @Test
    void getIsAnyOf_returns_empty_set_by_default() {
        final GeneralPropertyPredicate<String> predicate = GeneralPropertyPredicate.<String>builder().build();

        final Set<String> result = predicate.getIsAnyOf();

        assertThat(result).isEmpty();
    }

    @Test
    void getIsNoneOf_returns_empty_set_by_default() {
        final GeneralPropertyPredicate<String> predicate = GeneralPropertyPredicate.<String>builder().build();

        final Set<String> result = predicate.getIsNoneOf();

        assertThat(result).isEmpty();
    }

    @Test
    void getMatches_returns_empty_pattern_by_default() {
        final GeneralPropertyPredicate<String> predicate = GeneralPropertyPredicate.<String>builder().build();

        final Pattern result = predicate.getMatches();

        assertThat(result.pattern()).isEmpty();
    }

    @Test
    void getDoesNotMatch_returns_empty_pattern_by_default() {
        final GeneralPropertyPredicate<String> predicate = GeneralPropertyPredicate.<String>builder().build();

        final Pattern result = predicate.getDoesNotMatch();

        assertThat(result.pattern()).isEmpty();
    }

    @Test
    void test_given_null_returns_false_by_default() {
        final GeneralPropertyPredicate<String> predicate = GeneralPropertyPredicate.<String>builder().build();

        final boolean result = predicate.test(null);

        assertThat(result).isFalse();
    }

    @Test
    void test_given_nonnull_value_returns_true_by_default() {
        final GeneralPropertyPredicate<String> predicate = GeneralPropertyPredicate.<String>builder().build();

        final boolean result = predicate.test(TEST_STRING1);

        assertThat(result).isTrue();
    }

    @Test
    void test_given_null_returns_true_when_presence_is_OPTIONAL() {
        final GeneralPropertyPredicate<String> predicate = GeneralPropertyPredicate.<String>builder()//
                .setPresence(PresencePolicy.OPTIONAL)//
                .addIsAnyOf("requiredValue")//
                .build();

        final boolean result = predicate.test(null);

        assertThat(result).isTrue();
    }

    @Test
    void test_given_null_returns_true_when_presence_is_EMPTY() {
        final GeneralPropertyPredicate<String> predicate = GeneralPropertyPredicate.<String>builder()//
                .setPresence(PresencePolicy.EMPTY)//
                .build();

        final boolean result = predicate.test(null);

        assertThat(result).isTrue();
    }

    @Test
    void test_given_null_returns_false_when_presence_is_PRESENT() {
        final GeneralPropertyPredicate<String> predicate = GeneralPropertyPredicate.<String>builder()//
                .setPresence(PresencePolicy.PRESENT)//
                .build();

        final boolean result = predicate.test(null);

        assertThat(result).isFalse();
    }

    @Test
    void test_given_empty_Optional_returns_true_when_presence_is_EMPTY() {
        final GeneralPropertyPredicate<Optional<String>> predicate = GeneralPropertyPredicate.<Optional<String>>builder()//
                .setPresence(PresencePolicy.EMPTY)//
                .build();

        final boolean result = predicate.test(Optional.empty());

        assertThat(result).isTrue();
    }

    @Test
    void test_given_nonnull_value_returns_false_when_presence_is_EMPTY() {
        final GeneralPropertyPredicate<String> predicate = GeneralPropertyPredicate.<String>builder()//
                .setPresence(PresencePolicy.EMPTY)//
                .build();

        final boolean result = predicate.test(TEST_STRING1);

        assertThat(result).isFalse();
    }

    @Test
    void test_given_Optional_with_value_returns_false_when_presence_is_EMPTY() {
        final GeneralPropertyPredicate<Optional<String>> predicate = GeneralPropertyPredicate.<Optional<String>>builder()//
                .setPresence(PresencePolicy.EMPTY)//
                .build();

        final boolean result = predicate.test(Optional.of(TEST_STRING1));

        assertThat(result).isFalse();
    }

    @Test
    void test_given_value_in_isAnyOf_returns_true() {
        final GeneralPropertyPredicate<String> predicate = GeneralPropertyPredicate.<String>builder()//
                .addIsAnyOf(TEST_STRING1, TEST_STRING2, TEST_STRING3)//
                .build();

        final boolean result = predicate.test(TEST_STRING2);

        assertThat(result).isTrue();
    }

    @Test
    void test_given_value_in_isNoneOf_returns_false() {
        final GeneralPropertyPredicate<String> predicate = GeneralPropertyPredicate.<String>builder()//
                .addIsNoneOf(TEST_STRING1, TEST_STRING2, TEST_STRING3)//
                .build();

        final boolean result = predicate.test(TEST_STRING2);

        assertThat(result).isFalse();
    }

    @Test
    void test_given_value_not_in_isNoneOf_returns_true() {
        final GeneralPropertyPredicate<String> predicate = GeneralPropertyPredicate.<String>builder()//
                .addIsNoneOf(TEST_STRING1, TEST_STRING2, TEST_STRING3)//
                .build();

        final boolean result = predicate.test(TEST_STRING4);

        assertThat(result).isTrue();
    }

    @Test
    void test_given_value_matching_matches_returns_true() {
        final GeneralPropertyPredicate<String> predicate = GeneralPropertyPredicate.<String>builder()//
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
        final GeneralPropertyPredicate<String> predicate = GeneralPropertyPredicate.<String>builder()//
                .setMatches(Pattern.compile(""))//
                .build();

        final boolean result = predicate.test("any string");

        assertThat(result).isTrue();
    }

    @Test
    void test_given_value_not_matching_matches_returns_true() {
        final GeneralPropertyPredicate<String> predicate = GeneralPropertyPredicate.<String>builder()//
                .setMatches(Pattern.compile("^.*String2$"))//
                .build();

        final boolean result = predicate.test(TEST_STRING1);

        assertThat(result).isFalse();
    }

    /**
     * Empty pattern is considered nonexistent, and therefore anything passes.
     */

    @Test
    void test_given_value_matching_doesNotMatch_returns_false() {
        final GeneralPropertyPredicate<String> predicate = GeneralPropertyPredicate.<String>builder()//
                .setDoesNotMatch(Pattern.compile("^.*String2$"))//
                .build();

        final boolean result = predicate.test(TEST_STRING2);

        assertThat(result).isFalse();
    }

    @Test
    void test_given_value_not_matching_setDoesNotMatch_returns_true() {
        final GeneralPropertyPredicate<String> predicate = GeneralPropertyPredicate.<String>builder()//
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
        final GeneralPropertyPredicate<String> predicate = GeneralPropertyPredicate.<String>builder()//
                .setDoesNotMatch(Pattern.compile(""))//
                .build();

        final boolean result = predicate.test("any string");

        assertThat(result).isTrue();
    }

    @Test
    void test_given_value_satisfying_all_conditions_returns_true() {
        final GeneralPropertyPredicate<String> predicate = GeneralPropertyPredicate.<String>builder()//
                .addIsAnyOf(TEST_STRING1, TEST_STRING2, TEST_STRING4, TEST_STRING5)//
                .addIsNoneOf(TEST_STRING4)//
                .setMatches(Pattern.compile("^.*String[1-4]$"))//
                .setDoesNotMatch(Pattern.compile("^.*String2$"))//
                .build();

        final boolean result = predicate.test(TEST_STRING1);

        assertThat(result).isTrue();
    }

    @ParameterizedTest
    @CsvSource({"testString2", "testString3", "testString4", "testString5"})
    void test_given_value_not_satisfying_all_conditions_returns_false(final String input) {
        final GeneralPropertyPredicate<String> predicate = GeneralPropertyPredicate.<String>builder()//
                .addIsAnyOf(TEST_STRING1, TEST_STRING2, TEST_STRING4, TEST_STRING5)//
                .addIsNoneOf(TEST_STRING4)//
                .setMatches(Pattern.compile("^.*String[1-4]$"))//
                .setDoesNotMatch(Pattern.compile("^.*String2$"))//
                .build();

        final boolean result = predicate.test(input);

        assertThat(result).isFalse();
    }

    @Test
    void builder_build_throws_exception_when_presence_is_EMPTY_and_isAnyOf_has_values() {
        final GeneralPropertyPredicate.Builder<?> builder = GeneralPropertyPredicate.builder()//
                .setPresence(PresencePolicy.EMPTY)//
                .addIsAnyOf(TEST_STRING1);

        assertThatIllegalStateException()//
                .isThrownBy(builder::build)//
                .withMessageContaining("presence")//
                .withMessageContaining(PresencePolicy.EMPTY.toString());
    }

    @Test
    void builder_build_throws_exception_when_presence_is_EMPTY_and_isNoneOf_has_values() {
        final GeneralPropertyPredicate.Builder<?> builder = GeneralPropertyPredicate.builder()//
                .setPresence(PresencePolicy.EMPTY)//
                .addIsNoneOf(TEST_STRING1);

        assertThatIllegalStateException()//
                .isThrownBy(builder::build)//
                .withMessageContaining("presence")//
                .withMessageContaining(PresencePolicy.EMPTY.toString());
    }

    @Test
    void builder_build_throws_exception_when_presence_is_EMPTY_and_matches_is_non_empty_pattern() {
        final GeneralPropertyPredicate.Builder<?> builder = GeneralPropertyPredicate.builder()//
                .setPresence(PresencePolicy.EMPTY)//
                .setMatches(TEST_PATTERN);

        assertThatIllegalStateException()//
                .isThrownBy(builder::build)//
                .withMessageContaining("presence")//
                .withMessageContaining(PresencePolicy.EMPTY.toString());
    }

    @Test
    void builder_build_throws_exception_when_presence_is_EMPTY_and_doesNotMatch_is_non_empty_pattern() {
        final GeneralPropertyPredicate.Builder<?> builder = GeneralPropertyPredicate.builder()//
                .setPresence(PresencePolicy.EMPTY)//
                .setDoesNotMatch(TEST_PATTERN);

        assertThatIllegalStateException()//
                .isThrownBy(builder::build)//
                .withMessageContaining("presence")//
                .withMessageContaining(PresencePolicy.EMPTY.toString());
    }

    @Test
    void builder_build_throws_exception_when_both_is_and_isAnyOf_were_invoked() {
        final GeneralPropertyPredicate.Builder<String> builder = GeneralPropertyPredicate.<String>builder()//
                .setIs(TEST_STRING1)//
                .setIsAnyOf(Collections.singleton(TEST_STRING2));

        assertThatIllegalStateException()//
                .isThrownBy(builder::build)//
                .withMessageMatching(".*\\bis\\b.*")//
                .withMessageMatching(".*\\bisAnyOf\\b.*");
    }

    @Test
    void builder_build_throws_exception_when_both_isNot_and_isNoneOf_were_invoked() {
        final GeneralPropertyPredicate.Builder<String> builder = GeneralPropertyPredicate.<String>builder()//
                .setIsNot(TEST_STRING1)//
                .setIsNoneOf(Collections.singleton(TEST_STRING2));

        assertThatIllegalStateException()//
                .isThrownBy(builder::build)//
                .withMessageMatching(".*\\bisNot\\b.*")//
                .withMessageMatching(".*\\bisNoneOf\\b.*");
    }

    @Test
    void builder_setIs_replaces_existing_values_with_given_value() {
        final GeneralPropertyPredicate.Builder<String> builder = GeneralPropertyPredicate.<String>builder()//
                .addIsAnyOf(TEST_STRING1, TEST_STRING2);
        final String replacement = TEST_STRING3;

        final Set<String> result = builder.setIs(replacement)//
                .getIsAnyOf();

        assertThat(result).containsExactlyInAnyOrder(replacement);
    }

    @Test
    void builder_setIsAnyOf_replaces_existing_values_with_given_values() {
        final GeneralPropertyPredicate.Builder<String> builder = GeneralPropertyPredicate.<String>builder()//
                .addIsAnyOf(TEST_STRING1, TEST_STRING2);
        final HashSet<String> replacement = new HashSet<>(Arrays.asList(TEST_STRING2, TEST_STRING3));

        final Set<String> result = builder.setIsAnyOf(replacement)//
                .getIsAnyOf();

        assertThat(result).containsExactlyInAnyOrderElementsOf(replacement);
    }

    @Test
    void builder_setIsNot_replaces_existing_values_with_given_value() {
        final GeneralPropertyPredicate.Builder<String> builder = GeneralPropertyPredicate.<String>builder()//
                .addIsNoneOf(TEST_STRING1, TEST_STRING2);
        final String replacement = TEST_STRING3;

        final Set<String> result = builder.setIsNot(replacement)//
                .getIsNoneOf();

        assertThat(result).containsExactlyInAnyOrder(replacement);
    }

    @Test
    void builder_setIsNoneOf_replaces_existing_values_with_given_values() {
        final GeneralPropertyPredicate.Builder<String> builder = GeneralPropertyPredicate.<String>builder()//
                .addIsNoneOf(TEST_STRING1, TEST_STRING2);
        final HashSet<String> replacement = new HashSet<>(Arrays.asList(TEST_STRING2, TEST_STRING3));

        final Set<String> result = builder.setIsNoneOf(replacement)//
                .getIsNoneOf();

        assertThat(result).containsExactlyInAnyOrderElementsOf(replacement);
    }

    @Test
    void builder_transform_returns_new_builder_with_values_in_isAnyOf_transformed() {
        final GeneralPropertyPredicate.Builder<Integer> builder = GeneralPropertyPredicate.<Integer>builder()//
                .addIsAnyOf(1, 2);

        final GeneralPropertyPredicate.Builder<String> result = builder.transform(Object::toString);

        //noinspection AssertBetweenInconvertibleTypes
        assertThat(result).isNotSameAs(builder);
        assertThat(result.getIsAnyOf()).containsExactlyInAnyOrder("1", "2");
    }

    @Test
    void builder_transform_returns_new_builder_with_values_in_isNoneOf_transformed() {
        final GeneralPropertyPredicate.Builder<Integer> builder = GeneralPropertyPredicate.<Integer>builder()//
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
    @EnumSource(PresencePolicy.class)
    void builder_transform_returns_new_builder_with_presence_as_is(final PresencePolicy presencePolicy) {
        final GeneralPropertyPredicate.Builder<?> builder = GeneralPropertyPredicate.builder()//
                .setPresence(presencePolicy);

        final GeneralPropertyPredicate.Builder<String> result = builder.transform(Object::toString);

        assertThat(result).isNotSameAs(builder);
        assertThat(result.getPresence()).isEqualTo(presencePolicy);
    }

    @Test
    void validate_passes_when_all_elements_of_isAnyOf_satisfy_validator() {
        final GeneralPropertyPredicate.Builder<String> builder = GeneralPropertyPredicate.<String>builder()//
                .addIsAnyOf(TEST_STRING1, TEST_STRING2);
        final Collection<String> validValues = Arrays.asList(TEST_STRING1, TEST_STRING2, TEST_STRING3);

        builder.validate(validValues::contains);
    }

    @Test
    void validate_throws_exception_when_not_all_elements_of_isAnyOf_satisfy_validator() {
        final GeneralPropertyPredicate.Builder<String> builder = GeneralPropertyPredicate.<String>builder()//
                .addIsAnyOf(TEST_STRING1, TEST_STRING2, TEST_STRING3);
        final Collection<String> validValues = Arrays.asList(TEST_STRING1, TEST_STRING3, TEST_STRING4);

        assertThatIllegalStateException()//
                .isThrownBy(() -> builder.validate(validValues::contains))//
                .withMessageContaining("isAnyOf")//
                .withMessageContaining(TEST_STRING2);
    }

    @Test
    void validate_passes_when_all_elements_of_isNoneOf_satisfy_validator() {
        final GeneralPropertyPredicate.Builder<String> builder = GeneralPropertyPredicate.<String>builder()//
                .addIsNoneOf(TEST_STRING1, TEST_STRING2);
        final Collection<String> validValues = Arrays.asList(TEST_STRING1, TEST_STRING2, TEST_STRING3);

        builder.validate(validValues::contains);
    }

    @Test
    void validate_throws_exception_when_not_all_elements_of_isNoneOf_satisfy_validator() {
        final GeneralPropertyPredicate.Builder<String> builder = GeneralPropertyPredicate.<String>builder()//
                .addIsNoneOf(TEST_STRING1, TEST_STRING2, TEST_STRING3);
        final Collection<String> validValues = Arrays.asList(TEST_STRING1, TEST_STRING3, TEST_STRING4);

        assertThatIllegalStateException()//
                .isThrownBy(() -> builder.validate(validValues::contains))//
                .withMessageContaining("isNoneOf")//
                .withMessageContaining(TEST_STRING2);
    }

    @Test
    void validate_passes_regardless_of_matches_and_doesNotMatch() {
        final GeneralPropertyPredicate.Builder<String> builder = GeneralPropertyPredicate.<String>builder()//
                .setMatches(Pattern.compile("matchPattern"))//
                .setDoesNotMatch(Pattern.compile("doesNotMatchPattern"));
        final Collection<String> validValues = Collections.singleton(TEST_STRING1);

        builder.validate(validValues::contains);
    }

    @Test
    void validate_throws_exception_regardless_of_matches_and_doesNotMatch() {
        final GeneralPropertyPredicate.Builder<String> builder = GeneralPropertyPredicate.<String>builder()//
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
    void validate_passes_regardless_of_presence_being_EMPTY() {
        final GeneralPropertyPredicate.Builder<String> builder = GeneralPropertyPredicate.<String>builder()//
                .setPresence(PresencePolicy.EMPTY);
        final Collection<String> validValues = Arrays.asList(TEST_STRING1, TEST_STRING2, TEST_STRING3);

        builder.validate(validValues::contains);
    }

    @Test
    void validate_throws_exception_regardless_of_presence_being_EMPTY() {
        final GeneralPropertyPredicate.Builder<String> builder = GeneralPropertyPredicate.<String>builder()//
                .addIsAnyOf(TEST_STRING2)//
                .setPresence(PresencePolicy.EMPTY);
        final Collection<String> validValues = Collections.singleton(TEST_STRING1);

        assertThatIllegalStateException()//
                .isThrownBy(() -> builder.validate(validValues::contains))//
                .withMessageContaining("isAnyOf")//
                .withMessageContaining(TEST_STRING2);
    }

    @Test
    void toString_returns_initially_only_default_presence() {
        final GeneralPropertyPredicate<Object> predicate = GeneralPropertyPredicate.builder()//
                .build();
        final String result = predicate//
                .toString();
        assertThat(result).isEqualTo(String.format(Locale.ROOT, "presence{%s}", predicate.getPresence()));
    }

    @ParameterizedTest
    @EnumSource(PresencePolicy.class)
    void toString_returns_presence_value(final PresencePolicy presencePolicy) {
        final String result = GeneralPropertyPredicate.builder()//
                .setPresence(presencePolicy)//
                .build()//
                .toString();
        assertThat(result).isEqualTo(String.format(Locale.ROOT, "presence{%s}", presencePolicy));
    }

    @Test
    void toString_returns_string_describing_only_condition() {
        final String result = GeneralPropertyPredicate.<Integer>builder()//
                .setPresence(PresencePolicy.EMPTY)//
                .build()//
                .toString();
        assertThat(result).isEqualTo("presence{EMPTY}");
    }

    @Test
    void toString_returns_string_describing_two_conditions() {
        final String result = GeneralPropertyPredicate.<Integer>builder()//
                .setPresence(PresencePolicy.PRESENT).addIsNoneOf(4, 5, 6)//
                .build()//
                .toString();
        assertThat(result).isEqualTo("presence{PRESENT} & isNoneOf{[4, 5, 6]}");
    }

    @Test
    void toString_returns_string_describing_all_conditions() {
        final String result = GeneralPropertyPredicate.<Integer>builder()//
                .setPresence(PresencePolicy.OPTIONAL).addIsAnyOf(1, 2, 3)//
                .addIsNoneOf(4, 5, 6)//
                .setMatches(Pattern.compile("^\\d$"))//
                .setDoesNotMatch(Pattern.compile("^[a-z]*$"))//
                .build()//
                .toString();
        assertThat(result).isEqualTo("presence{OPTIONAL} & isAnyOf{[1, 2, 3]} & isNoneOf{[4, 5, 6]} & matches{^\\d$} & doesNotMatch{^[a-z]*$}");
    }

    @Test
    void integer_between_inclusiveExclusive_bounds_returns_true() {
        final GeneralPropertyPredicate<Integer> predicate = GeneralPropertyPredicate.<Integer>builder()
                .setIsGreaterOrEqualTo(10)   // inclusive lower
                .setIsLessThan(20)           // exclusive upper
                .build();

        assertThat(predicate.test(10)).isTrue();
        assertThat(predicate.test(15)).isTrue();
        assertThat(predicate.test(19)).isTrue();
        assertThat(predicate.test(20)).isFalse();
        assertThat(predicate.test(9)).isFalse();
    }

    @Test
    void integer_upper_inclusive_and_lower_exclusive() {
        final GeneralPropertyPredicate<Integer> predicate = GeneralPropertyPredicate.<Integer>builder()
                .setIsGreaterThan(5)         // exclusive lower
                .setIsLessOrEqualTo(7)       // inclusive upper
                .build();

        assertThat(predicate.test(5)).isFalse();
        assertThat(predicate.test(6)).isTrue();
        assertThat(predicate.test(7)).isTrue();
        assertThat(predicate.test(8)).isFalse();
    }

    @Test
    void non_comparable_without_comparator_fails_bounds() {
        final GeneralPropertyPredicate<Box> predicate = GeneralPropertyPredicate.<Box>builder()
                .setIsGreaterThan(new Box(1))
                .build();

        // No comparator and Box is not Comparable
        assertThat(predicate.test(new Box(2))).isFalse();
    }

    @Test
    void non_comparable_without_comparator_but_no_bounds_passes_other_checks() {
        final GeneralPropertyPredicate<Box> predicate = GeneralPropertyPredicate.<Box>builder().build();
        assertThat(predicate.test(new Box(1))).isTrue();
    }


    @Test
    void custom_comparator_enables_bounds_for_non_comparable() {
        final Comparator<Box> byV = Comparator.comparingInt(b -> b.v);

        final GeneralPropertyPredicate<Box> predicate = GeneralPropertyPredicate.<Box>builder()
                .setComparator(byV)
                .setIsGreaterOrEqualTo(new Box(5))
                .setIsLessThan(new Box(10))
                .build();

        assertThat(predicate.test(new Box(4))).isFalse();
        assertThat(predicate.test(new Box(5))).isTrue();
        assertThat(predicate.test(new Box(9))).isTrue();
        assertThat(predicate.test(new Box(10))).isFalse();
    }

    @Test
    void builder_validate_checks_scalar_bounds() {
        final GeneralPropertyPredicate.Builder<Integer> builder = GeneralPropertyPredicate.<Integer>builder()
                .setIsGreaterOrEqualTo(1)
                .setIsLessThan(100);

        builder.validate(v -> v != null && v > 0);
    }

    @Test
    void builder_validate_throws_when_scalar_bound_invalid() {
        final GeneralPropertyPredicate.Builder<Integer> builder = GeneralPropertyPredicate.<Integer>builder()
                .setIsLessOrEqualTo(-1);

        assertThatIllegalStateException()
                .isThrownBy(() -> builder.validate(v -> v != null && v >= 0))
                .withMessageContaining("isLessOrEqualTo")
                .withMessageContaining("-1");
    }

    @Test
    void builder_build_throws_when_presence_EMPTY_and_any_bound_is_set() {
        final GeneralPropertyPredicate.Builder<Integer> builder = GeneralPropertyPredicate.<Integer>builder()
                .setPresence(PresencePolicy.EMPTY)
                .setIsGreaterThan(0);

        assertThatIllegalStateException()
                .isThrownBy(builder::build)
                .withMessageContaining("presence")
                .withMessageContaining(PresencePolicy.EMPTY.toString());
    }

    @Test
    void builder_transform_carries_over_comparable_bounds() {
        final GeneralPropertyPredicate.Builder<Integer> intBuilder = GeneralPropertyPredicate.<Integer>builder()
                .setIsGreaterOrEqualTo(10)
                .setIsLessThan(20);

        final GeneralPropertyPredicate.Builder<String> stringBuilder = intBuilder.transform(Object::toString);

        assertThat(stringBuilder.getIsGreaterOrEqualTo()).isEqualTo("10");
        assertThat(stringBuilder.getIsLessThan()).isEqualTo("20");
    }

    @Test
    void toString_includes_bounds_conditions() {
        final String result = GeneralPropertyPredicate.<Integer>builder()
                .setIsGreaterThan(1)
                .setIsLessOrEqualTo(3)
                .build()
                .toString();

        assertThat(result).isEqualTo("presence{PRESENT} & isLessOrEqualTo{3} & isGreaterThan{1}");
    }

    @Test
    void null_value_with_presence_PRESENT_fails_even_if_bounds_set() {
        final GeneralPropertyPredicate<Integer> predicate = GeneralPropertyPredicate.<Integer>builder()
                .setIsGreaterOrEqualTo(1)
                .build();
        assertThat(predicate.test(null)).isFalse();
    }

    @Test
    void null_value_with_presence_OPTIONAL_passes_even_if_bounds_set() {
        final GeneralPropertyPredicate<Integer> predicate = GeneralPropertyPredicate.<Integer>builder()
                .setPresence(GeneralPropertyPredicate.PresencePolicy.OPTIONAL)
                .setIsGreaterOrEqualTo(1)
                .build();
        assertThat(predicate.test(null)).isTrue();
    }

    @Test
    void only_upper_inclusive_bound() {
        final GeneralPropertyPredicate<Integer> predicate = GeneralPropertyPredicate.<Integer>builder()
                .setIsLessOrEqualTo(10)
                .build();
        assertThat(predicate.test(9)).isTrue();
        assertThat(predicate.test(10)).isTrue();
        assertThat(predicate.test(11)).isFalse();
    }

    @Test
    void only_lower_exclusive_bound() {
        final GeneralPropertyPredicate<Integer> predicate = GeneralPropertyPredicate.<Integer>builder()
                .setIsGreaterThan(0)
                .build();
        assertThat(predicate.test(0)).isFalse();
        assertThat(predicate.test(1)).isTrue();
    }

    @Test
    void contradictory_bounds_greaterOrEqual_to_X_and_lessThan_X_never_match() {
        final GeneralPropertyPredicate<Integer> predicate = GeneralPropertyPredicate.<Integer>builder()
                .setIsGreaterOrEqualTo(10)
                .setIsLessThan(10)
                .build();
        assertThat(predicate.test(9)).isFalse();
        assertThat(predicate.test(10)).isFalse();
        assertThat(predicate.test(11)).isFalse();
    }

    @Test
    void contradictory_bounds_greaterThan_X_and_lessOrEqual_to_X_never_match() {
        final GeneralPropertyPredicate<Integer> predicate = GeneralPropertyPredicate.<Integer>builder()
                .setIsGreaterThan(5)
                .setIsLessOrEqualTo(5)
                .build();
        assertThat(predicate.test(5)).isFalse();
        assertThat(predicate.test(4)).isFalse();
        assertThat(predicate.test(6)).isFalse();
    }

    private record Box(int v) {
        @Override
        public String toString() {
            return "Box(" + v + ")";
        }
    }

}
