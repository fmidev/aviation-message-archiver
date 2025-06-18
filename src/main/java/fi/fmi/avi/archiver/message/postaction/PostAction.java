package fi.fmi.avi.archiver.message.postaction;

import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.MessageProcessorContext;

public interface PostAction {

    void run(MessageProcessorContext context, ArchiveAviationMessage message);

}
