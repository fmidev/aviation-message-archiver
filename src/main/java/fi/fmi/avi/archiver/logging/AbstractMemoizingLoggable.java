package fi.fmi.avi.archiver.logging;

public abstract class AbstractMemoizingLoggable extends AbstractLoggable {
    private String string;

    protected abstract void appendOnceTo(final StringBuilder builder);

    @Override
    public final String toString() {
        if (string == null) {
            final StringBuilder builder = new StringBuilder(estimateLogStringLength());
            appendOnceTo(builder);
            final String string = builder.toString();
            this.string = string;
            return string;
        } else {
            return string;
        }
    }
}
