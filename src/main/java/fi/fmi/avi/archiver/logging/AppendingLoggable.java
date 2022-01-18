package fi.fmi.avi.archiver.logging;

/**
 * A {@link Loggable} that is able to append the log string to a {@link StringBuilder}.
 * When the log string of this object is built using {@code StringBuilder}, and it is being used as part of a longer log string also being built using
 * {@code StringBuilder}, the ability to append content directly eliminates need for intermediate {@code String} creation.
 */
public interface AppendingLoggable extends Loggable {
    /**
     * Append the log string content (which is returned by {@link #toString()}) to provided builder.
     *
     * @param builder
     *         a builder to append log string content to
     */
    void appendTo(StringBuilder builder);
}
