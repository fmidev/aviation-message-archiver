package fi.fmi.avi.archiver.message.modifier;

import fi.fmi.avi.archiver.file.FileAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;

@FunctionalInterface
public interface MessageModifier {

    void modify(FileAviationMessage fileAviationMessage, ArchiveAviationMessage.Builder aviationMessageBuilder);

}
