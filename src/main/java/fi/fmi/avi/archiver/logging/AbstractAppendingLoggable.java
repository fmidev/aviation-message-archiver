package fi.fmi.avi.archiver.logging;

public abstract class AbstractAppendingLoggable extends AbstractLoggable implements AppendingLoggable {
    protected abstract int estimateLogStringLength();

    @Override
    public final String logString() {
        final StringBuilder builder = new StringBuilder(estimateLogStringLength());
        appendTo(builder);
        return builder.toString();
    }
}
