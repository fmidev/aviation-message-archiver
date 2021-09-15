package fi.fmi.avi.archiver.spring.convert;

import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.core.convert.support.DefaultConversionService;

public class MapValuesToCollectionConverter implements GenericConverter, ConditionalGenericConverter, ConversionServiceAware {
    private static final Set<ConvertiblePair> CONVERTIBLE_TYPES = Collections.singleton(new ConvertiblePair(Map.class, Collection.class));
    private ConversionService conversionService;

    public MapValuesToCollectionConverter() {
        this(new DefaultConversionService());
        ((DefaultConversionService) this.conversionService).addConverter(this);
    }

    public MapValuesToCollectionConverter(final ConversionService conversionService) {
        this.conversionService = requireNonNull(conversionService, "conversionService");
    }

    private static TypeDescriptor mapValuesTypeDescriptor(final TypeDescriptor mapType) {
        return TypeDescriptor.collection(Collection.class, mapType.getMapValueTypeDescriptor());
    }

    @Override
    public boolean matches(final TypeDescriptor sourceType, final TypeDescriptor targetType) {
        return sourceType.isMap() //
                && targetType.isCollection() //
                && conversionService.canConvert(mapValuesTypeDescriptor(sourceType), targetType);
    }

    @Override
    public Set<ConvertiblePair> getConvertibleTypes() {
        return CONVERTIBLE_TYPES;
    }

    @Override
    public Object convert(final Object source, final TypeDescriptor sourceType, final TypeDescriptor targetType) {
        return conversionService.convert(((Map<?, ?>) source).values(), mapValuesTypeDescriptor(sourceType), targetType);
    }

    @Override
    public void setConversionService(final ConversionService conversionService) {
        this.conversionService = requireNonNull(conversionService, "conversionService");
    }
}
