package fi.fmi.avi.archiver.config;

import java.time.Clock;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;

import fi.fmi.avi.archiver.initializing.AviationProductsHolder;
import fi.fmi.avi.archiver.message.populator.AbstractMessagePopulatorFactory;
import fi.fmi.avi.archiver.message.populator.BulletinHeadingDataPopulator;
import fi.fmi.avi.archiver.message.populator.FileMetadataPopulator;
import fi.fmi.avi.archiver.message.populator.MessageDataPopulator;
import fi.fmi.avi.archiver.message.populator.MessagePopulator;
import fi.fmi.avi.archiver.message.populator.MessagePopulatorFactory;
import fi.fmi.avi.archiver.message.populator.MessagePopulatorHelper;
import fi.fmi.avi.archiver.message.populator.ReflectionMessagePopulatorFactory;
import fi.fmi.avi.archiver.message.populator.SpringConversionServicePropertyConverter;
import fi.fmi.avi.model.GenericAviationWeatherMessage;
import fi.fmi.avi.model.MessageType;

@Configuration
public class MessagePopulatorFactoryConfig {
    @Autowired
    private ConversionService conversionService;

    @Autowired
    private Clock clock;

    @Autowired
    private AviationProductsHolder aviationProductsHolder;

    @Resource(name = "messageTypeIds")
    private Map<MessageType, Integer> messageTypeIds;

    @Resource(name = "messageFormatIds")
    private Map<GenericAviationWeatherMessage.Format, Integer> messageFormatIds;

    @Bean
    public MessagePopulatorHelper messagePopulatorHelper() {
        return new MessagePopulatorHelper(clock);
    }

    @Bean
    public AbstractMessagePopulatorFactory.PropertyConverter messagePopulatorFactoryPropertyConverter() {
        return new SpringConversionServicePropertyConverter(conversionService);
    }

    private <T extends MessagePopulator> MessagePopulatorFactory<T> newFactory(final Class<T> type, final Object... constructorArgs) {
        return new ReflectionMessagePopulatorFactory<>(messagePopulatorFactoryPropertyConverter(), type, constructorArgs);
    }

    @Bean
    public MessagePopulatorFactory<FileMetadataPopulator> fileMetadataPopulatorFactory() {
        return newFactory(FileMetadataPopulator.class, aviationProductsHolder.getProducts());
    }

    @Bean
    public MessagePopulatorFactory<BulletinHeadingDataPopulator> bulletinHeadingDataPopulatorFactory() {
        return newFactory(BulletinHeadingDataPopulator.class, messagePopulatorHelper(), messageFormatIds, messageTypeIds);
    }

    @Bean
    public MessagePopulatorFactory<MessageDataPopulator> messageDataPopulatorFactory() {
        return newFactory(MessageDataPopulator.class, messagePopulatorHelper(), messageFormatIds, messageTypeIds);
    }
}
