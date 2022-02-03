package fi.fmi.avi.archiver.message.populator.conditional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import javax.annotation.Nullable;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;

@SuppressFBWarnings("UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR")
class ActivationConditionTest {
    @Mock
    private ActivationCondition condition1;
    @Mock
    private ActivationCondition condition2;
    @Mock
    private ActivationCondition condition3;
    private AutoCloseable mocks;

    private InputAviationMessage input;
    private ArchiveAviationMessage.Builder target;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        input = InputAviationMessage.builder().buildPartial();
        target = ArchiveAviationMessage.builder();
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    @Test
    void and_given_empty_list_returns_empty() {
        final Optional<ActivationCondition> result = ActivationCondition.and(Collections.emptyList());

        assertThat(result).isEmpty();
    }

    @Test
    void and_given_single_condition_returns_given_condition() {
        final Optional<ActivationCondition> result = ActivationCondition.and(Collections.singletonList(condition1));

        assertThat(result).hasValue(condition1);
    }

    @Test
    void and_given_conditions_all_returning_true_returns_condition_returning_true() {
        when(condition1.test(any(), any())).thenReturn(true);
        when(condition2.test(any(), any())).thenReturn(true);
        when(condition3.test(any(), any())).thenReturn(true);

        @Nullable
        final ActivationCondition resultCondition = ActivationCondition.and(Arrays.asList(condition1, condition2, condition3)).orElse(null);
        assertThat(resultCondition).isNotNull();
        final boolean testResult = resultCondition.test(input, target);
        assertThat(testResult).isTrue();
    }

    @Test
    void and_given_conditions_any_returning_false_returns_condition_returning_false() {
        when(condition1.test(any(), any())).thenReturn(true);
        when(condition2.test(any(), any())).thenReturn(false);
        when(condition3.test(any(), any())).thenReturn(true);

        @Nullable
        final ActivationCondition resultCondition = ActivationCondition.and(Arrays.asList(condition1, condition2, condition3)).orElse(null);
        assertThat(resultCondition).isNotNull();
        final boolean testResult = resultCondition.test(input, target);
        assertThat(testResult).isFalse();
    }

    @Test
    void and_given_conditions_all_returning_false_returns_condition_returning_false() {
        when(condition1.test(any(), any())).thenReturn(false);
        when(condition2.test(any(), any())).thenReturn(false);
        when(condition3.test(any(), any())).thenReturn(false);

        @Nullable
        final ActivationCondition resultCondition = ActivationCondition.and(Arrays.asList(condition1, condition2, condition3)).orElse(null);
        assertThat(resultCondition).isNotNull();
        final boolean testResult = resultCondition.test(input, target);
        assertThat(testResult).isFalse();
    }

    @Test
    void or_given_empty_list_returns_empty() {
        final Optional<ActivationCondition> result = ActivationCondition.or(Collections.emptyList());

        assertThat(result).isEmpty();
    }

    @Test
    void or_given_single_condition_returns_given_condition() {
        final Optional<ActivationCondition> result = ActivationCondition.or(Collections.singletonList(condition1));

        assertThat(result).hasValue(condition1);
    }

    @Test
    void or_given_conditions_all_returning_true_returns_condition_returning_true() {
        when(condition1.test(any(), any())).thenReturn(true);
        when(condition2.test(any(), any())).thenReturn(true);
        when(condition3.test(any(), any())).thenReturn(true);

        @Nullable
        final ActivationCondition resultCondition = ActivationCondition.or(Arrays.asList(condition1, condition2, condition3)).orElse(null);
        assertThat(resultCondition).isNotNull();
        final boolean testResult = resultCondition.test(input, target);
        assertThat(testResult).isTrue();
    }

    @Test
    void or_given_conditions_any_returning_true_returns_condition_returning_true() {
        when(condition1.test(any(), any())).thenReturn(false);
        when(condition2.test(any(), any())).thenReturn(true);
        when(condition3.test(any(), any())).thenReturn(false);

        @Nullable
        final ActivationCondition resultCondition = ActivationCondition.or(Arrays.asList(condition1, condition2, condition3)).orElse(null);
        assertThat(resultCondition).isNotNull();
        final boolean testResult = resultCondition.test(input, target);
        assertThat(testResult).isTrue();
    }

    @Test
    void or_given_conditions_all_returning_false_returns_condition_returning_false() {
        when(condition1.test(any(), any())).thenReturn(false);
        when(condition2.test(any(), any())).thenReturn(false);
        when(condition3.test(any(), any())).thenReturn(false);

        @Nullable
        final ActivationCondition resultCondition = ActivationCondition.or(Arrays.asList(condition1, condition2, condition3)).orElse(null);
        assertThat(resultCondition).isNotNull();
        final boolean testResult = resultCondition.test(input, target);
        assertThat(testResult).isFalse();
    }
}
