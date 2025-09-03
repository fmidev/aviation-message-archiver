package fi.fmi.avi.archiver.util.instantiation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Executable;
import java.lang.reflect.Type;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

public enum TestConfigValueConverter implements ConfigValueConverter {
    INSTANCE;

    @Nullable
    @Override
    public Object toParameterType(@Nullable final Object propertyConfigValue, final Executable targetExecutable, final int parameterIndex) {
        if (propertyConfigValue == null) {
            return null;
        }
        final Type parameterType = targetExecutable.getParameterTypes()[parameterIndex];
        return convert(propertyConfigValue, parameterType);
    }

    @Nullable
    @Override
    public Object toReturnValueType(@Nullable final Object propertyConfigValue, final Executable targetExecutable) {
        if (propertyConfigValue == null) {
            return null;
        }
        final Type parameterType = targetExecutable.getAnnotatedReturnType().getType();
        return convert(propertyConfigValue, parameterType);
    }

    private Object convert(final @Nonnull Object propertyConfigValue, final Type parameterType) {
        try {
            if (parameterType instanceof final Class<?> parameterClass
                    && parameterClass.isInstance(propertyConfigValue)) {
                return propertyConfigValue;
            }
            final String propertyConfigValueString = String.valueOf(propertyConfigValue);
            if (String.class.equals(parameterType)) {
                return propertyConfigValueString;
            } else if (int.class.equals(parameterType)) {
                return Integer.parseInt(propertyConfigValueString);
            } else if (long.class.equals(parameterType)) {
                return Long.parseLong(propertyConfigValueString);
            } else if (short.class.equals(parameterType)) {
                return Short.parseShort(propertyConfigValueString);
            } else if (double.class.equals(parameterType)) {
                return Double.parseDouble(propertyConfigValueString);
            } else if (float.class.equals(parameterType)) {
                return Float.parseFloat(propertyConfigValueString);
            } else if (boolean.class.equals(parameterType)) {
                return Boolean.parseBoolean(propertyConfigValueString);
            } else if (OptionalInt.class.equals(parameterType)) {
                return OptionalInt.of(Integer.parseInt(propertyConfigValueString));
            } else if (OptionalLong.class.equals(parameterType)) {
                return OptionalLong.of(Long.parseLong(propertyConfigValueString));
            } else if (OptionalDouble.class.equals(parameterType)) {
                return OptionalDouble.of(Double.parseDouble(propertyConfigValueString));
            } else if (OptionalType.OPTIONAL.isOptional(parameterType)
                    && OptionalType.OPTIONAL.getValueType(parameterType)
                    .map(String.class::isAssignableFrom)
                    .orElse(false)) {
                return Optional.of(propertyConfigValueString);
            } else {
                throw new IllegalArgumentException("Unable to convert [" + propertyConfigValue + "] to " + parameterType);
            }
        } catch (final RuntimeException exception) {
            throw new ConfigValueConversionException(exception);
        }
    }
}
