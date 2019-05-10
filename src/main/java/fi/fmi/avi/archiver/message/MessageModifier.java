package fi.fmi.avi.archiver.message;

public interface MessageModifier {

    AviationMessage modify(AviationMessage message);

}
