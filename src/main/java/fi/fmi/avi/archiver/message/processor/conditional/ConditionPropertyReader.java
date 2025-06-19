package fi.fmi.avi.archiver.message.processor.conditional;

import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessageOrBuilder;

import javax.annotation.Nullable;
import java.lang.reflect.Method;

/**
 * Implementations of this interface read a single property from {@link InputAviationMessage} or {@link ArchiveAviationMessageOrBuilder} for condition
 * evaluation.
 *
 * @param <T> type of property
 * @see PropertyActivationCondition
 */
public interface ConditionPropertyReader<T> {
    /**
     * Return a {@code Method} instance that has the concrete property type as it's return type. It is used for determining the generic type of the property.
     * The returned method doesn't need to be the actual getter method for the property. Only the return type of the returned method must be equal to return
     * type of the actual getter.
     *
     * @return a {@code Method} instance that has the concrete property type as it's return type
     */
    Method getValueGetterForType();

    /**
     * Return default name of the related property to be used by {@link ConditionPropertyReaderFactory}.
     *
     * @return default name of the related property
     */
    String getPropertyName();

    /**
     * Read the property value.
     *
     * @param input   input aviation message
     * @param message archive aviation message or builder
     * @return property value
     */
    @Nullable
    T readValue(InputAviationMessage input, ArchiveAviationMessageOrBuilder message);

    /**
     * Validate a property value.
     * If there is no restrictions for the value, this method returns always {@code true}.
     *
     * @param value value to validate
     * @return {@code true} if provided {@code value} is valid, {@code false} otherwise
     * @throws NullPointerException if provided {@code value} is {@code null}
     */
    boolean validate(final T value);

    /**
     * Return a string representation of this condition suitable for logging.
     *
     * @return a string representation of this condition suitable for logging
     */
    String toString();
}
