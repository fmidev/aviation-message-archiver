package fi.fmi.avi.archiver.message.populator;

import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;

import javax.annotation.Nullable;

import static java.util.Objects.requireNonNull;

public class MessageContentTrimmer implements MessagePopulator {

    @Override
    public void populate(@Nullable final InputAviationMessage inputAviationMessage, final ArchiveAviationMessage.Builder builder) {
        requireNonNull(builder, "builder");
        MessagePopulatorHelper.tryGet(builder, ArchiveAviationMessage.Builder::getMessage)
                .ifPresent(message -> builder.setMessage(message.trim()));
    }

}
