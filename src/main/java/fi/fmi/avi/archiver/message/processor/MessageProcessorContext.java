package fi.fmi.avi.archiver.message.processor;

import fi.fmi.avi.archiver.ProcessingContext;
import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessageOrBuilder;

/**
 * Context for processing or reading an {@link ArchiveAviationMessageOrBuilder} object.
 */
public interface MessageProcessorContext extends ProcessingContext {
    InputAviationMessage getInputMessage();
}
