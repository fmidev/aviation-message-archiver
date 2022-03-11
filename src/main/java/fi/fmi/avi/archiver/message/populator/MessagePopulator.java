package fi.fmi.avi.archiver.message.populator;

import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.MessageDiscardedException;

@FunctionalInterface
public interface MessagePopulator {
    void populate(final MessagePopulatingContext context, ArchiveAviationMessage.Builder target) throws MessageDiscardedException;
}
