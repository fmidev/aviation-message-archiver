package fi.fmi.avi.archiver.spring.convert;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.core.convert.support.DefaultConversionService;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * A Converter converting values of provided Map to a supported collection, converting the elements as well.
 *
 * <p>
 * This implementation uses internally a {@link ConversionService} to convert {@link Map#values()} collection into target collection.
 * </p>
 */
public class MapValuesToCollectionConverter implements ConditionalGenericConverter, ConversionServiceAware {
    private static final Set<ConvertiblePair> CONVERTIBLE_TYPES = Collections.singleton(new ConvertiblePair(Map.class, Collection.class));
    private ConversionService conversionService;

    /**
     * Constructs a new instance using {@link DefaultConversionService} having the new instance added as a converter to allow recursive conversions.
     */
    public MapValuesToCollectionConverter() {
        this(new DefaultConversionService());
        ((DefaultConversionService) this.conversionService).addConverter(this);
    }

    /**
     * Constructs a new instance using provided {@code ConversionService} for conversions.
     * Typically, the new instance should be added to provided ConversionService to enable recursive conversions.
     *
     * @param conversionService conversion service to be used for value conversions
     */
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
