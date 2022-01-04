package fi.fmi.avi.archiver.logging;

public abstract class AbstractLoggable implements Loggable {
    @Override
    public final String toString() {
        return logString();
    }
}
