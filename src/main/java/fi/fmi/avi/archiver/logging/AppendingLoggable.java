package fi.fmi.avi.archiver.logging;

public interface AppendingLoggable extends Loggable {
    void appendTo(StringBuilder builder);
}
