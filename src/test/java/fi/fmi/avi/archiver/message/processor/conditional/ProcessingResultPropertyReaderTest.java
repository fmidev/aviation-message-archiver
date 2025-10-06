package fi.fmi.avi.archiver.message.processor.conditional;

import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessageOrBuilder;
import fi.fmi.avi.archiver.message.ProcessingResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import javax.annotation.Nullable;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessingResultPropertyReaderTest {
    @Test
    void readValue_given_message_without_processing_result_returns_initial_status() {
        final InputAviationMessage input = InputAviationMessage.builder().buildPartial();
        final ArchiveAviationMessage.Builder message = ArchiveAviationMessage.builder();
        final ProcessingResultPropertyReader propertyReader = new ProcessingResultPropertyReader();

        final ProcessingResult result = propertyReader.readValue(input, message);

        assertThat(result).isEqualTo(message.getProcessingResult());
    }

    @ParameterizedTest
    @EnumSource(ProcessingResult.class)
    void readValue_given_message_with_processing_result_returns_the_status(final ProcessingResult processingResult) {
        final InputAviationMessage input = InputAviationMessage.builder().buildPartial();
        final ArchiveAviationMessage.Builder message = ArchiveAviationMessage.builder()//
                .setProcessingResult(processingResult);
        final ProcessingResultPropertyReader propertyReader = new ProcessingResultPropertyReader();

        final ProcessingResult result = propertyReader.readValue(input, message);

        assertThat(result).isEqualTo(processingResult);
    }

    @ParameterizedTest
    @EnumSource(ProcessingResult.class)
    void validate_given_any_value_returns_true(final ProcessingResult processingResult) {
        final ProcessingResultPropertyReader propertyReader = new ProcessingResultPropertyReader();

        final boolean result = propertyReader.validate(processingResult);

        assertThat(result).isTrue();
    }

    @Test
    void testGetValueGetterForType() {
        final class TestReader extends AbstractConditionPropertyReader<ProcessingResult> {
            @Nullable
            @Override
            public ProcessingResult readValue(final InputAviationMessage input, final ArchiveAviationMessageOrBuilder message) {
                return null;
            }
        }
        final ProcessingResultPropertyReader reader = new ProcessingResultPropertyReader();
        final TestReader controlReader = new TestReader();

        assertThat(reader.getValueGetterForType().getGenericReturnType()).isEqualTo(controlReader.getValueGetterForType().getGenericReturnType());
    }

    @Test
    void testGetPropertyName() {
        final class ProcessingResultPropertyReader extends ConditionPropertyReaderTests.AbstractTestStringConditionPropertyReader {
            public ProcessingResultPropertyReader() {
            }
        }
        final fi.fmi.avi.archiver.message.processor.conditional.ProcessingResultPropertyReader reader //
                = new fi.fmi.avi.archiver.message.processor.conditional.ProcessingResultPropertyReader();
        final ProcessingResultPropertyReader controlReader = new ProcessingResultPropertyReader();

        assertThat(reader.getPropertyName()).isEqualTo(controlReader.getPropertyName());
    }

    @Test
    void testToString() {
        final ProcessingResultPropertyReader propertyReader = new ProcessingResultPropertyReader();

        assertThat(propertyReader.toString()).isEqualTo(propertyReader.getPropertyName());
    }
}
