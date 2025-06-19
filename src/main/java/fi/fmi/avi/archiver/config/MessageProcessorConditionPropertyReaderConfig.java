package fi.fmi.avi.archiver.config;

import com.google.common.collect.BiMap;
import fi.fmi.avi.archiver.config.model.AviationProduct;
import fi.fmi.avi.archiver.message.processor.conditional.*;
import fi.fmi.avi.archiver.message.processor.populator.BulletinHeadingSource;
import fi.fmi.avi.model.GenericAviationWeatherMessage;
import fi.fmi.avi.model.MessageType;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.Map;

@Configuration
public class MessageProcessorConditionPropertyReaderConfig {
    private final ConfigurableApplicationContext applicationContext;

    public MessageProcessorConditionPropertyReaderConfig(final ConfigurableApplicationContext applicationContext) {
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
