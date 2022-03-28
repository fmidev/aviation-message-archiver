package fi.fmi.avi.archiver.message.populator;

import javax.annotation.Nullable;

import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.MessageDiscardedException;
import fi.fmi.avi.archiver.message.populator.conditional.ConditionalMessagePopulator;

/**
 * Discard all messages.
 * This populator is typically composed as {@link ConditionalMessagePopulator} to limit affected messages.
 */
public class MessageDiscarder implements MessagePopulator {
    @Override
    public void populate(@Nullable final MessagePopulatingContext context, @Nullable final ArchiveAviationMessage.Builder target)
            throws MessageDiscardedException {
        throw new MessageDiscardedException("Discarded");
    }
}
