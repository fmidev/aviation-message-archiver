package fi.fmi.avi.archiver.message.processor.populator;

import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.ProcessingResult;
import fi.fmi.avi.archiver.message.processor.MessageProcessorContext;
import fi.fmi.avi.archiver.message.processor.TestMessageProcessorContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FixedProcessingResultPopulatorTest {
    @Test
    void populate_sets_fixed_processing_result() {
        final ProcessingResult processingResult = ProcessingResult.UNKNOWN_STATION_ICAO_CODE;
        final MessageProcessorContext context = TestMessageProcessorContext.create(InputAviationMessage.builder().buildPartial());
        final ArchiveAviationMessage.Builder target = ArchiveAviationMessage.builder();
        assertThat(target.getProcessingResult()).isNotEqualTo(processingResult);
        final FixedProcessingResultPopulator messagePopulator = new FixedProcessingResultPopulator(processingResult);

        messagePopulator.populate(context, target);

        assertThat(target.getProcessingResult()).isEqualTo(processingResult);
    }
}
