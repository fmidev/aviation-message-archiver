package fi.fmi.avi.archiver.logging;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * This interface denotes, that the {@link Loggable} object implementing this interface is additionally suitable for structured logging, e.g. can be formatted
 * as JSON.
 */
public interface StructuredLoggable extends Loggable {
    /**
     * Return a copy of this loggable, which represents a snapshot of current state, that can be read asynchronously later.
     * The purpose of this method is to support asynchronous logging.
     * The returned instance may be a read-only copy, or a mutable object, and doesn't have to be an instance of the same class.
     * Though extending interfaces should override this method to return an object implementing the interface in question.
     * If this instance is already immutable, the same instance may be returned.
     *
     * @return a readable snapshot copy of this object
     */
    StructuredLoggable readableCopy();

    /**
     * Returns default name for this structured object.
     * When no other name for a structured loggable object is provided by context, this name is used. This is the case when this object is the root
     * object, and not a named property of another object.
     *
     * <p>
     * The recommended implementation for this method is:
     * </p>
     *
     * <pre><code>
     * private static final String STRUCTURE_NAME = StructuredLoggables.defaultStructureName(MyStructuredLoggable.class);
     * &#64;Override
     * public String getStructureName() {
     *     return STRUCTURE_NAME;
     * }
     * </code></pre>
     *
     * <p>
     * Alternatively, if a specific name is desired, the method should simply return the desired {@code String} literal.
     * </p>
     *
     * @return default name for this structured object
     */
    @JsonIgnore
    String getStructureName();
}
