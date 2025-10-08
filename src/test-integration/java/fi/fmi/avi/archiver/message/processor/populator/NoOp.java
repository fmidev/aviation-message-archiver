package fi.fmi.avi.archiver.message.processor.populator;

import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.MessageDiscardedException;
import fi.fmi.avi.archiver.message.processor.MessageProcessorContext;

import static java.util.Objects.requireNonNull;

public class NoOp implements MessagePopulator {
    @Override
    public void populate(final MessageProcessorContext context, final ArchiveAviationMessage.Builder target) throws MessageDiscardedException {
        requireNonNull(context, "context");
        requireNonNull(target, "target");
    }

    public void setDummyInt(final int value) {
    }
}
