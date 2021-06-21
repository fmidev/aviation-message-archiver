package fi.fmi.avi.archiver.message.modifier;

import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;

@FunctionalInterface
public interface MessageModifier {

    void modify(InputAviationMessage InputAviationMessage, ArchiveAviationMessage.Builder aviationMessageBuilder);

}
