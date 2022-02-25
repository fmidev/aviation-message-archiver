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
     * When no other name for a structured loggable object is provided by context, this name can be used. This is the case when this object is the root
     * object, and not a named property of another object.
     *
     * <p>
     * The default implementation returns the {@link Class#getSimpleName() simple class name} with first character converted to lower case.
     * For {@link com.google.auto.value.AutoValue} and {@link org.inferred.freebuilder.FreeBuilder} classes the name of declaration class is returned,
     * not the generated class name.
     * </p>
     *
     * @return default name for this structured object
     */
    @JsonIgnore
    default String getStructureName() {
        return StructuredLoggableInternals.structureName(getClass());
    }
}
