package fi.fmi.avi.archiver.message.processor.conditional;

import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchivalStatus;
import fi.fmi.avi.archiver.message.ArchiveAviationMessageOrBuilder;

import static java.util.Objects.requireNonNull;

public class ArchivalStatusPropertyReader extends AbstractConditionPropertyReader<ArchivalStatus> {
    @Override
    public ArchivalStatus readValue(final InputAviationMessage input, final ArchiveAviationMessageOrBuilder message) {
        requireNonNull(input, "input");
        requireNonNull(message, "message");
        return message.getArchivalStatus();
    }
}
