package fi.fmi.avi.archiver.message.populator;

import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;

@FunctionalInterface
public interface MessagePopulator {

    void populate(InputAviationMessage inputAviationMessage, ArchiveAviationMessage.Builder aviationMessageBuilder);

}
