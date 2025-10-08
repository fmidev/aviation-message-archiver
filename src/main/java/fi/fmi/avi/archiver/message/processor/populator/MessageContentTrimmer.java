package fi.fmi.avi.archiver.message.processor.populator;

import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.processor.MessageProcessorContext;
import fi.fmi.avi.archiver.message.processor.MessageProcessorHelper;

import static java.util.Objects.requireNonNull;

/**
 * Apply a {@link String#trim() trim} operation on {@link ArchiveAviationMessage#getMessage() message content}.
 * This populator uses {@link ArchiveAviationMessage.Builder} as input, and therefore must be executed after message content is populated. In case message
 * content is missing, this populator does nothing.
 */
public class MessageContentTrimmer implements MessagePopulator {

    @Override
    public void populate(final MessageProcessorContext context, final ArchiveAviationMessage.Builder target) {
        requireNonNull(context, "context");
        requireNonNull(target, "target");
        MessageProcessorHelper.tryGet(target, ArchiveAviationMessage.Builder::getMessage)//
                .ifPresent(message -> target.setMessage(message.trim()));
    }

}
