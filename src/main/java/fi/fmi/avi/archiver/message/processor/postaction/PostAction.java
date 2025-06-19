package fi.fmi.avi.archiver.message.processor.postaction;

import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.processor.MessageProcessor;
import fi.fmi.avi.archiver.message.processor.MessageProcessorContext;

public interface PostAction extends MessageProcessor {

    void run(MessageProcessorContext context, ArchiveAviationMessage message);

}
