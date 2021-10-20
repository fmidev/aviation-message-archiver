package fi.fmi.avi.archiver.config;

import fi.fmi.avi.archiver.spring.convert.MapValuesToCollectionConverter;
import fi.fmi.avi.archiver.spring.convert.StringToDurationConverter;
import fi.fmi.avi.archiver.spring.convert.StringToInstantConverter;
import fi.fmi.avi.archiver.spring.convert.StringToPatternConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;

@Configuration
public class ConversionConfig {
    @Bean
    public ConversionService conversionService() {
        final DefaultConversionService conversionService = new DefaultConversionService();

        conversionService.addConverter(new StringToDurationConverter());
        conversionService.addConverter(new StringToInstantConverter());
        conversionService.addConverter(new StringToPatternConverter());
        conversionService.addConverter(new MapValuesToCollectionConverter(conversionService));

        return conversionService;
    }
}
