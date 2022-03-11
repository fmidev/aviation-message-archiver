package fi.fmi.avi.archiver.message.populator;

import javax.annotation.Nullable;

import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.MessageDiscardedException;

/**
 * Discard all messages.
 * Use within {@link fi.fmi.avi.archiver.message.populator.conditional.ConditionalMessagePopulator ConditionalMessagePopulator} to select messages to discard.
 */
public class MessageDiscarder implements MessagePopulator {
    @Override
    public void populate(@Nullable final MessagePopulatingContext context, @Nullable final ArchiveAviationMessage.Builder target)
            throws MessageDiscardedException {
        throw new MessageDiscardedException("Discarded");
    }
}
