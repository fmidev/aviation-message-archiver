package fi.fmi.avi.archiver.message;

@FunctionalInterface
public interface MessageModifier {

    AviationMessage.Builder modify(AviationMessage.Builder message);

}
