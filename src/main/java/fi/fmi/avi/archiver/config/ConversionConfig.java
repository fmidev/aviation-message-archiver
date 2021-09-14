package fi.fmi.avi.archiver.config;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ConversionServiceFactoryBean;
import org.springframework.core.convert.ConversionService;

import fi.fmi.avi.archiver.spring.convert.ConversionServiceAware;
import fi.fmi.avi.archiver.spring.convert.MapToListGenericConverter;
import fi.fmi.avi.archiver.spring.convert.StringToDurationConverter;
import fi.fmi.avi.archiver.spring.convert.StringToInstantConverter;

@Configuration
public class ConversionConfig {
    @Bean
    public ConversionService conversionService() {
        final ConversionServiceFactoryBean factoryBean = new ConversionServiceFactoryBean();

        final List<ConversionServiceAware> conversionServiceAwareConverters = createConversionServiceAwareConverters();
        final Set<Object> converters = createConverters(conversionServiceAwareConverters);
        factoryBean.setConverters(converters);

        factoryBean.afterPropertiesSet();
        final ConversionService conversionService = requireNonNull(factoryBean.getObject(), "conversionService");

        for (final ConversionServiceAware conversionServiceAware : conversionServiceAwareConverters) {
            conversionServiceAware.setConversionService(conversionService);
        }

        return conversionService;
    }

    private Set<Object> createConverters(final List<ConversionServiceAware> conversionServiceAwareConverters) {
        final Set<Object> converters = new HashSet<>();
        converters.add(new StringToDurationConverter());
        converters.add(new StringToInstantConverter());
        converters.addAll(conversionServiceAwareConverters);
        return converters;
    }

    private List<ConversionServiceAware> createConversionServiceAwareConverters() {
        final List<ConversionServiceAware> conversionServiceAwares = new ArrayList<>();
        conversionServiceAwares.add(new MapToListGenericConverter());
        return conversionServiceAwares;
    }
}
