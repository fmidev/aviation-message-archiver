package fi.fmi.avi.archiver.message.processor.postaction;

import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.processor.MessageProcessorContext;

import static java.util.Objects.requireNonNull;

public class NoOp implements PostAction {
    @Override
    public void run(final MessageProcessorContext context, final ArchiveAviationMessage message) {
        requireNonNull(context, "context");
        requireNonNull(message, "message");
    }

    public void setDummyInt(final int value) {
    }
}
