package fi.fmi.avi.archiver.message;

@FunctionalInterface
public interface MessageModifier {

    AviationMessage modify(AviationMessage message);

}
