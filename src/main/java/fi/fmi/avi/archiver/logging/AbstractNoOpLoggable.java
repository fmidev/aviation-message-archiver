package fi.fmi.avi.archiver.logging;

import static java.util.Objects.requireNonNull;

public class AbstractNoOpLoggable extends AbstractLoggable implements AppendingLoggable {
    @Override
    public final void appendTo(final StringBuilder builder) {
        requireNonNull(builder, "builder");
        builder.append(logString());
    }

    @Override
    public String logString() {
        return "[omitted]";
    }
}
