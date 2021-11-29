package fi.fmi.avi.archiver.config;

import fi.fmi.avi.archiver.initializing.AviationProductsHolder;
import fi.fmi.avi.archiver.message.populator.*;
import fi.fmi.avi.model.GenericAviationWeatherMessage;
import fi.fmi.avi.model.MessageType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;

import javax.annotation.Resource;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

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
    public AbstractMessagePopulatorFactory.ConfigValueConverter messagePopulatorConfigValueConverter() {
        return new SpringConversionServiceConfigValueConverter(conversionService);
    }

    private <T extends MessagePopulator> ReflectionMessagePopulatorFactory.Builder<T> builder(final Class<T> type) {
        return ReflectionMessagePopulatorFactory.builder(type, messagePopulatorConfigValueConverter());
    }

    @Bean
    public MessagePopulatorFactory<FileMetadataPopulator> fileMetadataPopulatorFactory() {
        return builder(FileMetadataPopulator.class)//
                .addDependencyArg(aviationProductsHolder.getProducts())//
                .build();
    }

    @Bean
    public MessagePopulatorFactory<FileNameDataPopulator> fileNameDataPopulatorFactory() {
        return builder(FileNameDataPopulator.class)//
                .addDependencyArg(messagePopulatorHelper())//
                .build();
    }

    @Bean
    public MessagePopulatorFactory<BulletinHeadingDataPopulator> bulletinHeadingDataPopulatorFactory() {
        return builder(BulletinHeadingDataPopulator.class)//
                .addDependencyArgs(messagePopulatorHelper(), messageFormatIds, messageTypeIds)//
                .build();
    }

    @Bean
    public MessagePopulatorFactory<MessageDataPopulator> messageDataPopulatorFactory() {
        return builder(MessageDataPopulator.class)//
                .addDependencyArgs(messagePopulatorHelper(), messageFormatIds, messageTypeIds)//
                .build();
    }

    @Bean
    public MessagePopulatorFactory<FixedDurationValidityPeriodPopulator> fixedDurationValidityPeriodPopulatorFactory() {
        return builder(FixedDurationValidityPeriodPopulator.class)//
                .addDependencyArg(messageTypeIds)//
                .addConfigArg("messageType", MessageType.class)//
                .addConfigArg("validityEndOffset", Duration.class)//
                .build();
    }

    @Bean
    public MessagePopulatorFactory<MessageFutureTimeValidator> messageFutureTimeValidatorFactory() {
        return builder(MessageFutureTimeValidator.class)//
                .addDependencyArg(clock)//
                .addConfigArg("acceptInFuture", Duration.class)//
                .build();
    }

    @Bean
    public MessagePopulatorFactory<MessageMaximumAgeValidator> messageMaximumAgeValidatorFactory() {
        return builder(MessageMaximumAgeValidator.class)//
                .addDependencyArg(clock)//
                .addConfigArg("maximumAge", Duration.class)//
                .build();
    }

    @Bean
    public MessagePopulatorFactory<StationIcaoCodeReplacer> stationIcaoCodeReplacerFactory() {
        return builder(StationIcaoCodeReplacer.class)//
                .addConfigArg("pattern", Pattern.class)//
                .addConfigArg("replacement", String.class)//
                .build();
    }

    @Bean
    public MessagePopulatorFactory<StationIcaoCodeAuthorizer> stationIcaoCodeAuthorizerFactory() {
        return builder(StationIcaoCodeAuthorizer.class)//
                .addConfigArg("originatorPattern", Pattern.class)//
                .addConfigArg("stationPattern", Pattern.class)//
                .build();
    }

    @Bean
    public MessagePopulatorFactory<OriginatorAuthorizer> originatorAuthorizerFactory() {
        return builder(OriginatorAuthorizer.class)//
                .addConfigArg("originatorPattern", Pattern.class)//
                .addConfigArg("stationPattern", Pattern.class)//
                .build();
    }

    @Bean
    public MessagePopulatorFactory<ProductMessageTypesValidator> productMessageTypesValidatorFactory() {
        return builder(ProductMessageTypesValidator.class)//
                .addDependencyArg(messageTypeIds)//
                .addConfigArg("productIdentifier", String.class)//
                .addConfigArg("messageTypes", Set.class)//
                .build();
    }

    @Bean
    public MessagePopulatorFactory<DataDesignatorDiscarder> dataDesignatorDiscarderFactory() {
        return builder(DataDesignatorDiscarder.class)//
                .addConfigArg("pattern", Pattern.class)//
                .build();
    }

    @Bean
    public MessagePopulatorFactory<MessageContentTrimmer> messageContentTrimmerFactory() {
        return builder(MessageContentTrimmer.class).build();
    }

}
