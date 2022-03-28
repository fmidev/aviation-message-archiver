package fi.fmi.avi.archiver.message.populator;

import static java.util.Objects.requireNonNull;

import fi.fmi.avi.archiver.message.ArchiveAviationMessage;

/**
 * Apply a {@link String#trim() trim} operation on {@link ArchiveAviationMessage#getMessage() message content}.
 * This populator uses {@link ArchiveAviationMessage.Builder} as input, and therefore must be executed after message content is populated. In case message
 * content is missing, this populator does nothing.
 */
public class MessageContentTrimmer implements MessagePopulator {

    @Override
    public void populate(final MessagePopulatingContext context, final ArchiveAviationMessage.Builder target) {
        requireNonNull(context, "context");
        requireNonNull(target, "target");
        MessagePopulatorHelper.tryGet(target, ArchiveAviationMessage.Builder::getMessage)//
                .ifPresent(message -> target.setMessage(message.trim()));
    }

}
