package fi.fmi.avi.archiver.message.populator;

import static java.util.Objects.requireNonNull;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;

import javax.annotation.Nullable;

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;

public class SpringConversionServicePropertyConverter implements AbstractMessagePopulatorFactory.PropertyConverter {
    private final ConversionService conversionService;

    public SpringConversionServicePropertyConverter(final ConversionService conversionService) {
        this.conversionService = requireNonNull(conversionService, "conversionService");
    }

    @Nullable
    @Override
    public Object convert(@Nullable final Object propertyConfigValue, final Method setterMethod) {
        requireNonNull(setterMethod, "setterMethod");
        return propertyConfigValue == null
                ? null
                : conversionService.convert(propertyConfigValue, typeDescriptorForObject(propertyConfigValue),
                        new TypeDescriptor(new MethodParameter(setterMethod, 0)));
    }

    @Nullable
    private TypeDescriptor typeDescriptorForObject(@Nullable final Object object) {
        if (object instanceof Collection && !((Collection<?>) object).isEmpty()) {
            final Collection<?> collection = (Collection<?>) object;
            return TypeDescriptor.collection(collection.getClass(), typeDescriptorForObject(collection.iterator().next()));
        } else if (object instanceof Map && !((Map<?, ?>) object).isEmpty()) {
            final Map<?, ?> map = (Map<?, ?>) object;
            final Map.Entry<?, ?> entry = ((Map<?, ?>) object).entrySet().iterator().next();
            return TypeDescriptor.map(map.getClass(), typeDescriptorForObject(entry.getKey()), typeDescriptorForObject(entry.getValue()));
        } else {
            return TypeDescriptor.forObject(object);
        }
    }
}
