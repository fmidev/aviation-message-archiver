package fi.fmi.avi.archiver.message.populator;

import static java.util.Objects.requireNonNull;

import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.ProcessingResult;
import fi.fmi.avi.archiver.message.populator.conditional.ConditionalMessagePopulator;

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
    public void populate(final MessagePopulatingContext context, final ArchiveAviationMessage.Builder target) {
        requireNonNull(context, "context");
        requireNonNull(target, "target");
        target.setProcessingResult(processingResult);
    }
}
