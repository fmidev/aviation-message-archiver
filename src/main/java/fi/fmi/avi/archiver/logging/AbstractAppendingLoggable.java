package fi.fmi.avi.archiver.logging;

public abstract class AbstractAppendingLoggable extends AbstractLoggable implements AppendingLoggable {
    @Override
    public final String toString() {
        final StringBuilder builder = new StringBuilder(estimateLogStringLength());
        appendTo(builder);
        return builder.toString();
    }
}
