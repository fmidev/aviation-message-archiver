package fi.fmi.avi.archiver.spring.convert;

import fi.fmi.avi.archiver.util.instantiation.ObjectFactoryConfig;
import fi.fmi.avi.archiver.util.instantiation.ObjectFactoryConfigFactory;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;

import java.util.Map;
import java.util.Set;

import static java.util.Objects.requireNonNull;

public class MapToObjectFactoryConfigConverter implements ConditionalGenericConverter {
    private final ObjectFactoryConfigFactory objectFactoryConfigFactory;

    public MapToObjectFactoryConfigConverter(final ObjectFactoryConfigFactory objectFactoryConfigFactory) {
        this.objectFactoryConfigFactory = requireNonNull(objectFactoryConfigFactory, "objectFactoryConfigFactory");
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean matches(final TypeDescriptor sourceType, final TypeDescriptor targetType) {
        final Class<?> targetClass = targetType.getType();
        return sourceType.isMap()
                && ObjectFactoryConfig.class.isAssignableFrom(targetClass)
                && objectFactoryConfigFactory.isValidConfigType((Class<? extends ObjectFactoryConfig>) targetClass);
    }

    @Override
    public Set<ConvertiblePair> getConvertibleTypes() {
        return Set.of(new ConvertiblePair(Map.class, ObjectFactoryConfig.class));
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object convert(final Object source, final TypeDescriptor sourceType, final TypeDescriptor targetType) {
        if (source == null) {
            return null;
        }
        return objectFactoryConfigFactory.create((Class<? extends ObjectFactoryConfig>) targetType.getType(), (Map<?, ?>) source);
    }
}
