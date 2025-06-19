package fi.fmi.avi.archiver.config;

import fi.fmi.avi.archiver.util.instantiation.ConfigValueConverter;
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
}
