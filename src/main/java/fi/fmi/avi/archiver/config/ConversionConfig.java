package fi.fmi.avi.archiver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;

import fi.fmi.avi.archiver.spring.convert.MapValuesToCollectionConverter;
import fi.fmi.avi.archiver.spring.convert.StringToDurationConverter;
import fi.fmi.avi.archiver.spring.convert.StringToGeneralPropertyPredicateBuilderConverter;
import fi.fmi.avi.archiver.spring.convert.StringToInstantConverter;
import fi.fmi.avi.archiver.spring.convert.StringToPatternConverter;

@Configuration
public class ConversionConfig {

    @Bean
    ConversionService conversionService() {
        final DefaultConversionService conversionService = new DefaultConversionService();

        conversionService.addConverter(new MapValuesToCollectionConverter(conversionService));
        conversionService.addConverter(new StringToDurationConverter());
        conversionService.addConverter(new StringToGeneralPropertyPredicateBuilderConverter());
        conversionService.addConverter(new StringToInstantConverter());
        conversionService.addConverter(new StringToPatternConverter());

        return conversionService;
    }

}
