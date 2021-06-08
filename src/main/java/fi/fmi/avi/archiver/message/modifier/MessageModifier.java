package fi.fmi.avi.archiver.message.modifier;

import fi.fmi.avi.archiver.message.AviationMessage;

@FunctionalInterface
public interface MessageModifier {

    AviationMessage.Builder modify(AviationMessage.Builder message);

}
