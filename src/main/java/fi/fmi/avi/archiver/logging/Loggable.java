package fi.fmi.avi.archiver.logging;

/**
 * This interface denotes, that an object implementing it is designed for logging as is.
 *
 * <p>
 * As the log string is produced by the {@link #toString()} method, it enables to use this object as a log string parameter without need to invoke any
 * methods to generate the loggable string. Instead, the string, if computed lazily, will be computed only, when the logging level in question is actually
 * enabled in the logging configuration, without any need to test it explicitly.
 * </p>
 *
 * <p>
 * Any implementing class <strong>must</strong> override the {@link #toString()} method. Instead of implementing this interface directly, it is recommended
 * to extend {@link AbstractLoggable}, which enforces overriding {@code toString()}.
 * </p>
 */
public interface Loggable {
    /**
     * Return a quick estimate of the log string length returned by {@link #toString()}.
     * Main use case for this method is to estimate initial capacity of {@link StringBuilder} when being used as part of a longer log string.
     *
     * @return estimated length of log string returned by {@code toString()}.
     */
    int estimateLogStringLength();

    /**
     * Returns string representation of this object suitable for logging.
     *
     * @return a loggable string
     */
    String toString();
}
