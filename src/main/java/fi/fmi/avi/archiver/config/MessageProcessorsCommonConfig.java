package fi.fmi.avi.archiver.config;

import fi.fmi.avi.archiver.spring.convert.MapToObjectFactoryConfigConverter;
import fi.fmi.avi.archiver.util.instantiation.ConfigValueConverter;
import fi.fmi.avi.archiver.util.instantiation.ObjectFactoryConfigFactory;
import fi.fmi.avi.archiver.util.instantiation.ProxyObjectFactoryConfigFactory;
import fi.fmi.avi.archiver.util.instantiation.SpringConversionServiceConfigValueConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;

import static java.util.Objects.requireNonNull;

@Configuration
public class MessageProcessorsCommonConfig {
    private final ConversionService conversionService;

    MessageProcessorsCommonConfig(final ConversionService conversionService) {
        this.conversionService = requireNonNull(conversionService, "conversionService");
    }

    @Bean
    ConfigValueConverter configValueConverter() {
        return new SpringConversionServiceConfigValueConverter(conversionService);
    }

    /**
     * Init {@code ObjectFactoryConfigFactory}.
     *
     * <p>
     * Direct nested config is disabled, but indirect nested config is enabled via
     * {@link MapToObjectFactoryConfigConverter} initialized in {@link ConversionPostConstructs#postConstruct()},
     * used by {@code configValueConverter}.
     * </p>
     *
     * @param configValueConverter configValueConverter
     * @return objectFactoryConfigFactory
     */
    @Bean
    ObjectFactoryConfigFactory objectFactoryConfigFactory(final ConfigValueConverter configValueConverter) {
        return new ProxyObjectFactoryConfigFactory(configValueConverter, false);
    }
}
