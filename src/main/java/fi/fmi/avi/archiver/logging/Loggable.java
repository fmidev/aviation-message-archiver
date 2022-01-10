package fi.fmi.avi.archiver.logging;

public interface Loggable {
    int estimateLogStringLength();

    String toString();
}
