package fi.fmi.avi.archiver.message.populator;

import static java.util.Objects.requireNonNull;

import fi.fmi.avi.archiver.message.ArchiveAviationMessage;

public class MessageContentTrimmer implements MessagePopulator {

    @Override
    public void populate(final MessagePopulatingContext context, final ArchiveAviationMessage.Builder target) {
        requireNonNull(context, "context");
        requireNonNull(target, "target");
        MessagePopulatorHelper.tryGet(target, ArchiveAviationMessage.Builder::getMessage)//
                .ifPresent(message -> target.setMessage(message.trim()));
    }

}
