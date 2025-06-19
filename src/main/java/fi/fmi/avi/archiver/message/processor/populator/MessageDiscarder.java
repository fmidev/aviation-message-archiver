package fi.fmi.avi.archiver.message.processor.populator;

import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.MessageDiscardedException;
import fi.fmi.avi.archiver.message.processor.MessageProcessorContext;

import javax.annotation.Nullable;

/**
 * Discard all messages.
 * This populator is typically composed as {@link ConditionalMessagePopulator} to limit affected messages.
 */
public class MessageDiscarder implements MessagePopulator {
    @Override
    public void populate(@Nullable final MessageProcessorContext context, @Nullable final ArchiveAviationMessage.Builder target)
            throws MessageDiscardedException {
        throw new MessageDiscardedException("Discarded");
    }
}
