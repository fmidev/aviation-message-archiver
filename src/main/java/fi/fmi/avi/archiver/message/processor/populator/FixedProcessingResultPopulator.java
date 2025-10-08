package fi.fmi.avi.archiver.message.processor.populator;

import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.ProcessingResult;
import fi.fmi.avi.archiver.message.processor.MessageProcessorContext;

import static java.util.Objects.requireNonNull;

/**
 * Set a fixed {@link ArchiveAviationMessage#getProcessingResult() processing result} on message.
 * This populator is typically composed as {@link ConditionalMessagePopulator} to limit affected messages.
 */
public class FixedProcessingResultPopulator implements MessagePopulator {
    private final ProcessingResult processingResult;

    public FixedProcessingResultPopulator(final ProcessingResult processingResult) {
        this.processingResult = requireNonNull(processingResult, "processingResult");
    }

    @Override
    public void populate(final MessageProcessorContext context, final ArchiveAviationMessage.Builder target) {
        requireNonNull(context, "context");
        requireNonNull(target, "target");
        target.setProcessingResult(processingResult);
    }
}
