package fi.fmi.avi.archiver.message.populator.conditional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.Predicate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;

@SuppressFBWarnings("UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR")
class PropertyActivationConditionTest {
    @Mock
    private ConditionPropertyReader<String> conditionPropertyReader;
    @Mock
    private Predicate<String> propertyPredicate;
    private AutoCloseable mocks;

    private InputAviationMessage input;
    private ArchiveAviationMessage.Builder target;
    private PropertyActivationCondition<String> condition;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        input = InputAviationMessage.builder().buildPartial();
        target = ArchiveAviationMessage.builder();
        condition = new PropertyActivationCondition<>(conditionPropertyReader, propertyPredicate);
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    @ParameterizedTest
    @CsvSource({ "true", "false" })
    void test_invokes_predicate_on_value_returned_by_conditionPropertyReader(final boolean expectedResult) {
        final String value = "testPropertyValue";
        when(conditionPropertyReader.readValue(input, target)).thenReturn(value);
        when(propertyPredicate.test(value)).thenReturn(expectedResult);

        final boolean result = condition.test(input, target);

        verify(conditionPropertyReader).readValue(input, target);
        verify(propertyPredicate).test(value);
        assertThat(result).isEqualTo(expectedResult);
    }
}
