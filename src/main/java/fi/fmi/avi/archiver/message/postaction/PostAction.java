package fi.fmi.avi.archiver.message.postaction;

import fi.fmi.avi.archiver.logging.model.ReadableLoggingContext;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;

public interface PostAction {

    void run(ReadableLoggingContext context, ArchiveAviationMessage message);

}
