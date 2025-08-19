package fi.fmi.avi.archiver.util.instantiation;

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;

import javax.annotation.Nullable;
import java.lang.reflect.Executable;
import java.util.Collection;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * A {@code ConfigValueConverter} implementation delegating conversion to Spring {@link ConversionService}.
 */
public class SpringConversionServiceConfigValueConverter implements ConfigValueConverter {
    private final ConversionService conversionService;

    public SpringConversionServiceConfigValueConverter(final ConversionService conversionService) {
        this.conversionService = requireNonNull(conversionService, "conversionService");
    }

    @Nullable
    @Override
    public Object toParameterType(@Nullable final Object propertyConfigValue, final Executable targetExecutable, final int parameterIndex) {
        requireNonNull(targetExecutable, "targetExecutable");
        if (parameterIndex < 0) {
            throw new IllegalArgumentException("parameterIndex must not be negative; was: " + parameterIndex);
        }
        return convert(propertyConfigValue, targetExecutable, parameterIndex);
    }

    @Nullable
    @Override
    public Object toReturnValueType(@Nullable final Object propertyConfigValue, final Executable targetExecutable) {
        requireNonNull(targetExecutable, "targetExecutable");
        return convert(propertyConfigValue, targetExecutable, -1);
    }

    @Nullable
    private Object convert(final @Nullable Object propertyConfigValue, final Executable targetExecutable, final int parameterIndex) {
        try {
            return propertyConfigValue == null
                    ? null
                    : conversionService.convert(propertyConfigValue, typeDescriptorForObject(propertyConfigValue),
                    new TypeDescriptor(MethodParameter.forExecutable(targetExecutable, parameterIndex)));
        } catch (final ConversionException exception) {
            throw new ConfigValueConversionException(exception);
        }
    }

    @Nullable
    private TypeDescriptor typeDescriptorForObject(@Nullable final Object object) {
        if (object instanceof final Collection<?> collection && !collection.isEmpty()) {
            return TypeDescriptor.collection(collection.getClass(), typeDescriptorForObject(collection.iterator().next()));
        } else if (object instanceof final Map<?, ?> map && !map.isEmpty()) {
            final Map.Entry<?, ?> entry = map.entrySet().iterator().next();
            return TypeDescriptor.map(map.getClass(), typeDescriptorForObject(entry.getKey()), typeDescriptorForObject(entry.getValue()));
        } else {
            return TypeDescriptor.forObject(object);
        }
    }
}
