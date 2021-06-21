package fi.fmi.avi.archiver.message.modifier;

import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;

@FunctionalInterface
public interface MessagePopulator {

    void modify(InputAviationMessage inputAviationMessage, ArchiveAviationMessage.Builder aviationMessageBuilder);

}
