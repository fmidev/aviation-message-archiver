package fi.fmi.avi.archiver.message.processor.populator;

import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.processor.MessageProcessorContext;

import static java.util.Objects.requireNonNull;

public class MessageAppendingPopulator implements MessagePopulator {
    private final String content;

    public MessageAppendingPopulator(final String content) {
        this.content = requireNonNull(content, "content");
    }

    @Override
    public void populate(final MessageProcessorContext context, final ArchiveAviationMessage.Builder target) {
        requireNonNull(context, "context");
        requireNonNull(target, "target");
        target.mapMessage(message -> message + "; " + content);
    }
}
