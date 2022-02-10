package fi.fmi.avi.archiver.config;

import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.collect.BiMap;

import fi.fmi.avi.archiver.config.model.AviationProduct;
import fi.fmi.avi.archiver.message.populator.BulletinHeadingSource;
import fi.fmi.avi.archiver.message.populator.conditional.ConditionPropertyReader;
import fi.fmi.avi.archiver.message.populator.conditional.DataDesignatorPropertyReader;
import fi.fmi.avi.archiver.message.populator.conditional.FormatPropertyReader;
import fi.fmi.avi.archiver.message.populator.conditional.OriginatorPropertyReader;
import fi.fmi.avi.archiver.message.populator.conditional.ProductIdPropertyReader;
import fi.fmi.avi.archiver.message.populator.conditional.RoutePropertyReader;
import fi.fmi.avi.archiver.message.populator.conditional.StationPropertyReader;
import fi.fmi.avi.archiver.message.populator.conditional.TypePropertyReader;
import fi.fmi.avi.model.GenericAviationWeatherMessage;
import fi.fmi.avi.model.MessageType;

@Configuration
public class MessagePopulatorConditionPropertyReaderConfig {
    private final ConfigurableApplicationContext applicationContext;

    public MessagePopulatorConditionPropertyReaderConfig(final ConfigurableApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Bean
    FormatPropertyReader formatPropertyReader(final BiMap<GenericAviationWeatherMessage.Format, Integer> messageFormatIds) {
        return new FormatPropertyReader(messageFormatIds);
    }

    @Bean
    ProductIdPropertyReader productIdentifierPropertyReader(final Map<String, AviationProduct> aviationProducts) {
        return new ProductIdPropertyReader(aviationProducts);
    }

    @Bean
    RoutePropertyReader routePropertyReader(final BiMap<String, Integer> messageRouteIds) {
        return new RoutePropertyReader(messageRouteIds);
    }

    @Bean
    StationPropertyReader stationPropertyReader() {
        return new StationPropertyReader();
    }

    @Bean
    TypePropertyReader typePropertyReader(final BiMap<MessageType, Integer> messageTypeIds) {
        return new TypePropertyReader(messageTypeIds);
    }

    @PostConstruct
    void registerBulletinHeadingConditionPropertyReaders() {
        BulletinHeadingSource.getPermutations().forEach(bulletinHeadingSources -> {
            register(new DataDesignatorPropertyReader(bulletinHeadingSources));
            register(new OriginatorPropertyReader(bulletinHeadingSources));
        });

    }

    private void register(final ConditionPropertyReader<?> conditionPropertyReader) {
        final String beanName = conditionPropertyReader.getClass().getSimpleName() + "." + conditionPropertyReader.getPropertyName();
        applicationContext.getBeanFactory().registerSingleton(beanName, conditionPropertyReader);
    }
}
