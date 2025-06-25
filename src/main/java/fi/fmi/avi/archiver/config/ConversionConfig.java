package fi.fmi.avi.archiver.config;

import fi.fmi.avi.archiver.spring.convert.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;

@Configuration
public class ConversionConfig {

    @Bean
    ConversionService conversionService() {
        final DefaultConversionService conversionService = new DefaultConversionService();

        conversionService.addConverter(new EmptyStringToEmptyMapConverter());
        conversionService.addConverter(new MapValuesToCollectionConverter(conversionService));
        conversionService.addConverter(new StringToDurationConverter());
        conversionService.addConverter(new StringToInstantConverter());
        conversionService.addConverter(new StringToPatternConverter());

        return conversionService;
    }

}
