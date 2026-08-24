package fi.fmi.avi.archiver.message.processor.populator;

import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.MessageDiscardedException;
import fi.fmi.avi.archiver.message.processor.MessageProcessorContext;
import org.jspecify.annotations.Nullable;

/**
 * Discard all messages.
 * This populator is typically composed as {@link ConditionalMessagePopulator} to limit affected messages.
 */
public class MessageDiscarder implements MessagePopulator {
    @Override
    public void populate(final @Nullable MessageProcessorContext context, final ArchiveAviationMessage.@Nullable Builder target)
            throws MessageDiscardedException {
        throw new MessageDiscardedException("Discarded");
    }
}
