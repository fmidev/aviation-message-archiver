package fi.fmi.avi.archiver.logging;

import static java.util.Objects.requireNonNull;

public abstract class AbstractNoOpLoggable extends AbstractLoggable implements AppendingLoggable {
    @Override
    public final void appendTo(final StringBuilder builder) {
        requireNonNull(builder, "builder");
        builder.append(this);
    }

    @Override
    public final int estimateLogStringLength() {
        return toString().length();
    }

    @Override
    public String toString() {
        return "unavailable";
    }
}
