package fi.fmi.avi.archiver.logging;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * This interface denotes, that the {@link Loggable} object implementing this interface is additionally suitable for structured logging, e.g. can be formatted
 * as JSON.
 */
public interface StructuredLoggable extends Loggable {
    /**
     * Return default {@link StructuredLoggable#getStructureName() structure name} for a {@code StructuredLoggable} class.
     *
     * @param forClass
     *         class to get default structure name for
     *
     * @return default structure name for profided {@code forClass}
     */
    static String defaultStructureName(final Class<? extends StructuredLoggable> forClass) {
        final Class<? extends StructuredLoggable> classForName = StructuredLoggableInternals.resolveClassForStructureName(forClass);
        final String simpleName = classForName.getSimpleName();
        if (simpleName.isEmpty()) {
            final String className = classForName.getName();
            return StructuredLoggableInternals.startingWithLowerCase(className.substring(className.lastIndexOf('.') + 1).replace('$', '_'));
        } else {
            return StructuredLoggableInternals.startingWithLowerCase(simpleName);
        }
    }

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
     * private static final String STRUCTURE_NAME = StructuredLoggable.defaultStructureName(MyStructuredLoggable.class);
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
