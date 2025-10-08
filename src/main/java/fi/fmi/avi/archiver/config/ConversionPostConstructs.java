package fi.fmi.avi.archiver.config;

import fi.fmi.avi.archiver.spring.convert.MapToObjectFactoryConfigConverter;
import fi.fmi.avi.archiver.util.instantiation.ObjectFactoryConfigFactory;
import fi.fmi.avi.archiver.util.instantiation.PropertyRenamingObjectFactoryConfigFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.support.ConfigurableConversionService;

import javax.annotation.PostConstruct;

/**
 * Adds converters to {@link ConversionConfig#conversionService() conversionService} that would cause circular
 * dependencies if instantiated within the conversion service bean declaration.
 */
@Configuration
public class ConversionPostConstructs {
    private final ConfigurableConversionService conversionService;
    private final ObjectFactoryConfigFactory objectFactoryConfigFactory;

    public ConversionPostConstructs(
            final ConfigurableConversionService conversionService,
            final ObjectFactoryConfigFactory objectFactoryConfigFactory) {
        this.conversionService = conversionService;
        this.objectFactoryConfigFactory = objectFactoryConfigFactory;
    }

    @PostConstruct
    public void postConstruct() {
        final PropertyRenamingObjectFactoryConfigFactory decoratedObjectFactoryConfigFactory = new PropertyRenamingObjectFactoryConfigFactory(
                objectFactoryConfigFactory, AbstractMessageProcessorFactoryConfig.CONFIG_PROPERTY_RENAME_OPERATOR);
        conversionService.addConverter(new MapToObjectFactoryConfigConverter(decoratedObjectFactoryConfigFactory));
    }
}
