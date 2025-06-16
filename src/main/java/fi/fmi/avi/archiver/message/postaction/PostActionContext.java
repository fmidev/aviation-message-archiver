package fi.fmi.avi.archiver.message.postaction;

import fi.fmi.avi.archiver.logging.model.ReadableLoggingContext;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;

/**
 * Context for post-actions that will be provided as input to all instances in {@link PostAction} execution chain.
 */
public interface PostActionContext {

    ReadableLoggingContext getLoggingContext();

    ArchiveAviationMessage getArchiveMessage();

}
