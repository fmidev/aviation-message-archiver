package fi.fmi.avi.archiver.message.populator;

import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.logging.model.ReadableLoggingContext;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;

/**
 * Context for populating an {@link ArchiveAviationMessage} object, that will be provided as input to all instances in {@link MessagePopulator} execution chain.
 */
public interface MessagePopulatingContext {
    ReadableLoggingContext getLoggingContext();

    InputAviationMessage getInputMessage();
}
