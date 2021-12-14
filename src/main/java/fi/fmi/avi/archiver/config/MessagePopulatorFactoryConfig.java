package fi.fmi.avi.archiver.config;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;

import fi.fmi.avi.archiver.config.model.AviationProduct;
import fi.fmi.avi.archiver.message.populator.AbstractMessagePopulatorFactory;
import fi.fmi.avi.archiver.message.populator.BulletinHeadingDataPopulator;
import fi.fmi.avi.archiver.message.populator.DataDesignatorDiscarder;
import fi.fmi.avi.archiver.message.populator.FileMetadataPopulator;
import fi.fmi.avi.archiver.message.populator.FileNameDataPopulator;
import fi.fmi.avi.archiver.message.populator.FixedDurationValidityPeriodPopulator;
import fi.fmi.avi.archiver.message.populator.MessageContentTrimmer;
import fi.fmi.avi.archiver.message.populator.MessageDataPopulator;
import fi.fmi.avi.archiver.message.populator.MessageFutureTimeValidator;
import fi.fmi.avi.archiver.message.populator.MessageMaximumAgeValidator;
import fi.fmi.avi.archiver.message.populator.MessagePopulator;
import fi.fmi.avi.archiver.message.populator.MessagePopulatorFactory;
import fi.fmi.avi.archiver.message.populator.MessagePopulatorHelper;
import fi.fmi.avi.archiver.message.populator.OriginatorAuthorizer;
import fi.fmi.avi.archiver.message.populator.ProductMessageTypesValidator;
import fi.fmi.avi.archiver.message.populator.ReflectionMessagePopulatorFactory;
import fi.fmi.avi.archiver.message.populator.SpringConversionServiceConfigValueConverter;
import fi.fmi.avi.archiver.message.populator.StationIcaoCodeAuthorizer;
import fi.fmi.avi.archiver.message.populator.StationIcaoCodeReplacer;
import fi.fmi.avi.model.GenericAviationWeatherMessage;
import fi.fmi.avi.model.MessageType;

@Configuration
public class MessagePopulatorFactoryConfig {

    private final ConversionService conversionService;
    private final Clock clock;
    private final Map<String, AviationProduct> aviationProducts;
    private final Map<MessageType, Integer> messageTypeIds;
    private final Map<GenericAviationWeatherMessage.Format, Integer> messageFormatIds;

    public MessagePopulatorFactoryConfig(final ConversionService conversionService, final Clock clock, final Map<String, AviationProduct> aviationProducts,
            @Qualifier("messageTypeIds") final Map<MessageType, Integer> messageTypeIds,
            @Qualifier("messageFormatIds") final Map<GenericAviationWeatherMessage.Format, Integer> messageFormatIds) {
        this.conversionService = requireNonNull(conversionService, "conversionService");
        this.clock = requireNonNull(clock, "clock");
        this.aviationProducts = requireNonNull(aviationProducts, "aviationProducts");
        this.messageTypeIds = requireNonNull(messageTypeIds, "messageTypeIds");
        this.messageFormatIds = requireNonNull(messageFormatIds, "messageFormatIds");

        checkArgument(!messageTypeIds.isEmpty(), "messageTypeIds cannot be empty");
        checkArgument(!messageFormatIds.isEmpty(), "messageFormatIds cannot be empty");
    }

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
                .addDependencyArg(aviationProducts)//
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
