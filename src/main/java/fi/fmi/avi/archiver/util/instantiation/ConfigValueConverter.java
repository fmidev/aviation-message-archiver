package fi.fmi.avi.archiver.util.instantiation;

import javax.annotation.Nullable;
import java.lang.reflect.Executable;

public interface ConfigValueConverter {
    /**
     * Convert provided {@code propertyConfigValue} to type of {@code targetExecutable} parameter at index {@code parameterIndex}.
     * May throw any type of {@code RuntimeException} if conversion fails.
     *
     * @param propertyConfigValue value to convert
     * @param targetExecutable    an {@code Executable} holding target type as its parameter
     * @param parameterIndex      index of {@code Executable} parameter for target type
     * @return converted value
     * @throws ConfigValueConversionException if conversion fails
     */
    @Nullable
    Object toParameterType(@Nullable final Object propertyConfigValue, final Executable targetExecutable, final int parameterIndex);

    /**
     * Convert provided {@code propertyConfigValue} to type of {@code targetExecutable} return value.
     * May throw any type of {@code RuntimeException} if conversion fails.
     *
     * @param propertyConfigValue value to convert
     * @param targetExecutable    an {@code Executable} holding target type as its return value
     * @return converted value
     * @throws ConfigValueConversionException if conversion fails
     */
    @Nullable
    Object toReturnValueType(@Nullable final Object propertyConfigValue, final Executable targetExecutable);
}
