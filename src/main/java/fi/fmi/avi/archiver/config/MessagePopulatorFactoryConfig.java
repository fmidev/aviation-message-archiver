package fi.fmi.avi.archiver.config;

import static java.util.Objects.requireNonNull;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;

import fi.fmi.avi.archiver.config.model.AviationProduct;
import fi.fmi.avi.archiver.config.model.MessagePopulatorFactory;
import fi.fmi.avi.archiver.message.populator.BulletinHeadingDataPopulator;
import fi.fmi.avi.archiver.message.populator.FileMetadataPopulator;
import fi.fmi.avi.archiver.message.populator.FileNameDataPopulator;
import fi.fmi.avi.archiver.message.populator.FixedDurationValidityPeriodPopulator;
import fi.fmi.avi.archiver.message.populator.FixedRoutePopulator;
import fi.fmi.avi.archiver.message.populator.FixedTypePopulator;
import fi.fmi.avi.archiver.message.populator.MessageContentTrimmer;
import fi.fmi.avi.archiver.message.populator.MessageDataPopulator;
import fi.fmi.avi.archiver.message.populator.MessageDiscarder;
import fi.fmi.avi.archiver.message.populator.MessageFutureTimeValidator;
import fi.fmi.avi.archiver.message.populator.MessageMaximumAgeValidator;
import fi.fmi.avi.archiver.message.populator.MessagePopulator;
import fi.fmi.avi.archiver.message.populator.MessagePopulatorHelper;
import fi.fmi.avi.archiver.message.populator.OriginatorAuthorizer;
import fi.fmi.avi.archiver.message.populator.ProductMessageTypesValidator;
import fi.fmi.avi.archiver.message.populator.StationIcaoCodeAuthorizer;
import fi.fmi.avi.archiver.message.populator.StationIcaoCodeReplacer;
import fi.fmi.avi.archiver.util.instantiation.ConfigValueConverter;
import fi.fmi.avi.archiver.util.instantiation.ReflectionObjectFactory;
import fi.fmi.avi.archiver.util.instantiation.SpringConversionServiceConfigValueConverter;
import fi.fmi.avi.model.GenericAviationWeatherMessage;
import fi.fmi.avi.model.MessageType;

@Configuration
public class MessagePopulatorFactoryConfig {

    private final ConversionService conversionService;
    private final Clock clock;

    MessagePopulatorFactoryConfig(final ConversionService conversionService, final Clock clock) {
        this.conversionService = requireNonNull(conversionService, "conversionService");
        this.clock = requireNonNull(clock, "clock");
    }

    @Bean
    MessagePopulatorHelper messagePopulatorHelper() {
        return new MessagePopulatorHelper(clock);
    }

    @Bean
    ConfigValueConverter messagePopulatorConfigValueConverter() {
        return new SpringConversionServiceConfigValueConverter(conversionService);
    }

    private <T extends MessagePopulator> ReflectionObjectFactory.Builder<T> builder(final Class<T> type) {
        return ReflectionObjectFactory.builder(type, messagePopulatorConfigValueConverter());
    }

    private <T extends MessagePopulator> MessagePopulatorFactory<T> build(final ReflectionObjectFactory.Builder<T> builder) {
        return new MessagePopulatorFactory<>(builder.build());
    }

    @Bean
    MessagePopulatorFactory<FileMetadataPopulator> fileMetadataPopulatorFactory(final Map<String, AviationProduct> aviationProducts) {
        return build(builder(FileMetadataPopulator.class)//
                .addDependencyArg(aviationProducts));
    }

    @Bean
    MessagePopulatorFactory<FileNameDataPopulator> fileNameDataPopulatorFactory() {
        return build(builder(FileNameDataPopulator.class)//
                .addDependencyArg(messagePopulatorHelper())//
                .addDependencyArg(clock));
    }

    @Bean
    MessagePopulatorFactory<BulletinHeadingDataPopulator> bulletinHeadingDataPopulatorFactory(final Map<MessageType, Integer> messageTypeIds,
            final Map<GenericAviationWeatherMessage.Format, Integer> messageFormatIds) {
        return build(builder(BulletinHeadingDataPopulator.class)//
                .addDependencyArgs(messagePopulatorHelper(), messageFormatIds, messageTypeIds));
    }

    @Bean
    MessagePopulatorFactory<MessageDataPopulator> messageDataPopulatorFactory(final Map<MessageType, Integer> messageTypeIds,
            final Map<GenericAviationWeatherMessage.Format, Integer> messageFormatIds) {
        return build(builder(MessageDataPopulator.class)//
                .addDependencyArgs(messagePopulatorHelper(), messageFormatIds, messageTypeIds));
    }

    @Bean
    MessagePopulatorFactory<FixedDurationValidityPeriodPopulator> fixedDurationValidityPeriodPopulatorFactory(final Map<MessageType, Integer> messageTypeIds) {
        return build(builder(FixedDurationValidityPeriodPopulator.class)//
                .addDependencyArg(messageTypeIds)//
                .addConfigArg("messageType", MessageType.class)//
                .addConfigArg("validityEndOffset", Duration.class));
    }

    @Bean
    MessagePopulatorFactory<FixedRoutePopulator> fixedRoutePopulatorFactory(final Map<String, Integer> routeIds) {
        return build(builder(FixedRoutePopulator.class)//
                .addDependencyArgs(routeIds)//
                .addConfigArg("route", String.class));
    }

    @Bean
    MessagePopulatorFactory<MessageFutureTimeValidator> messageFutureTimeValidatorFactory() {
        return build(builder(MessageFutureTimeValidator.class)//
                .addDependencyArg(clock)//
                .addConfigArg("acceptInFuture", Duration.class));
    }

    @Bean
    MessagePopulatorFactory<MessageMaximumAgeValidator> messageMaximumAgeValidatorFactory() {
        return build(builder(MessageMaximumAgeValidator.class)//
                .addDependencyArg(clock)//
                .addConfigArg("maximumAge", Duration.class));
    }

    @Bean
    MessagePopulatorFactory<StationIcaoCodeReplacer> stationIcaoCodeReplacerFactory() {
        return build(builder(StationIcaoCodeReplacer.class)//
                .addConfigArg("pattern", Pattern.class)//
                .addConfigArg("replacement", String.class));
    }

    @Bean
    MessagePopulatorFactory<StationIcaoCodeAuthorizer> stationIcaoCodeAuthorizerFactory() {
        return build(builder(StationIcaoCodeAuthorizer.class)//
                .addConfigArg("originatorPattern", Pattern.class)//
                .addConfigArg("stationPattern", Pattern.class));
    }

    @Bean
    MessagePopulatorFactory<OriginatorAuthorizer> originatorAuthorizerFactory() {
        return build(builder(OriginatorAuthorizer.class)//
                .addConfigArg("originatorPattern", Pattern.class)//
                .addConfigArg("stationPattern", Pattern.class));
    }

    @Bean
    MessagePopulatorFactory<ProductMessageTypesValidator> productMessageTypesValidatorFactory(final Map<MessageType, Integer> messageTypeIds,
            final Map<String, AviationProduct> aviationProducts) {
        return build(builder(ProductMessageTypesValidator.class)//
                .addDependencyArg(messageTypeIds)//
                .addDependencyArg(aviationProducts)//
                .addConfigArg("productMessageTypes", Map.class));
    }

    @Bean
    MessagePopulatorFactory<FixedTypePopulator> typePopulatorFactory(final Map<MessageType, Integer> typeIds) {
        return build(builder(FixedTypePopulator.class)//
                .addDependencyArgs(typeIds)//
                .addConfigArg("type", MessageType.class));
    }

    @Bean
    MessagePopulatorFactory<MessageDiscarder> messageDiscarderFactory() {
        return build(builder(MessageDiscarder.class));
    }

    @Bean
    MessagePopulatorFactory<MessageContentTrimmer> messageContentTrimmerFactory() {
        return build(builder(MessageContentTrimmer.class));
    }

}
