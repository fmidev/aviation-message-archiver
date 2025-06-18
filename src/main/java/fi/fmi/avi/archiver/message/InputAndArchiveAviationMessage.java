package fi.fmi.avi.archiver.message;

import fi.fmi.avi.archiver.file.InputAviationMessage;

import static java.util.Objects.requireNonNull;

public record InputAndArchiveAviationMessage(
        InputAviationMessage inputMessage,
        ArchiveAviationMessage archiveMessage) {

    public InputAndArchiveAviationMessage {
        requireNonNull(inputMessage, "inputMessage");
        requireNonNull(archiveMessage, "archiveMessage");
    }

    public InputAndArchiveAviationMessage withArchiveMessage(final ArchiveAviationMessage archiveMessage) {
        requireNonNull(archiveMessage, "archiveMessage");
        return new InputAndArchiveAviationMessage(inputMessage, archiveMessage);
    }
}
