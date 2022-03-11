package fi.fmi.avi.archiver.message.populator;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.ProcessingResult;

class FixedProcessingResultPopulatorTest {
    @Test
    void populate_sets_fixed_processing_result() {
        final ProcessingResult processingResult = ProcessingResult.UNKNOWN_STATION_ICAO_CODE;
        final MessagePopulatingContext context = TestMessagePopulatingContext.create(InputAviationMessage.builder().buildPartial());
        final ArchiveAviationMessage.Builder target = ArchiveAviationMessage.builder();
        assertThat(target.getProcessingResult()).isNotEqualTo(processingResult);
        final FixedProcessingResultPopulator messagePopulator = new FixedProcessingResultPopulator(processingResult);

        messagePopulator.populate(context, target);

        assertThat(target.getProcessingResult()).isEqualTo(processingResult);
    }
}
