package fi.fmi.avi.archiver.message.processor.conditional;

import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessageOrBuilder;
import fi.fmi.avi.archiver.message.ProcessingResult;

import static java.util.Objects.requireNonNull;

public class ProcessingResultPropertyReader extends AbstractConditionPropertyReader<ProcessingResult> {
    @Override
    public ProcessingResult readValue(final InputAviationMessage input, final ArchiveAviationMessageOrBuilder message) {
        requireNonNull(input, "input");
        requireNonNull(message, "message");
        return message.getProcessingResult();
    }
}
