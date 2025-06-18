package fi.fmi.avi.archiver.message;

import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.logging.model.ReadableLoggingContext;

/**
 * Context for processing or reading an {@link ArchiveAviationMessageOrBuilder} object.
 */
public interface MessageProcessorContext {
    ReadableLoggingContext getLoggingContext();

    InputAviationMessage getInputMessage();
}
