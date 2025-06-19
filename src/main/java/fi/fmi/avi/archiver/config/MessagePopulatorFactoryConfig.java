package fi.fmi.avi.archiver.config;

import fi.fmi.avi.archiver.config.model.AviationProduct;
import fi.fmi.avi.archiver.config.model.MessagePopulatorFactory;
import fi.fmi.avi.archiver.message.ProcessingResult;
import fi.fmi.avi.archiver.message.processor.populator.*;
import fi.fmi.avi.archiver.util.StringCaseFormat;
import fi.fmi.avi.archiver.util.instantiation.ConfigValueConverter;
import fi.fmi.avi.archiver.util.instantiation.PropertyRenamingObjectFactory;
import fi.fmi.avi.archiver.util.instantiation.ReflectionObjectFactory;
import fi.fmi.avi.model.GenericAviationWeatherMessage;
import fi.fmi.avi.model.MessageType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;

@Configuration
public class MessagePopulatorFactoryConfig {

    private final ConfigValueConverter configValueConverter;
    private final Clock clock;

    MessagePopulatorFactoryConfig(final ConfigValueConverter configValueConverter, final Clock clock) {
        this.configValueConverter = requireNonNull(configValueConverter, "configValueConverter");
        this.clock = requireNonNull(clock, "clock");
    }

    @Bean
    MessagePopulatorHelper messagePopulatorHelper() {
        return new MessagePopulatorHelper(clock);
    }

    private <T extends MessagePopulator> ReflectionObjectFactory.Builder<T> builder(final Class<T> type) {
        return ReflectionObjectFactory.builder(type, configValueConverter);
    }

    private <T extends MessagePopulator> MessagePopulatorFactory<T> build(final ReflectionObjectFactory.Builder<T> builder) {
        return new MessagePopulatorFactory<>(new PropertyRenamingObjectFactory<>(builder.build(), StringCaseFormat::dashToCamel));
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
    MessagePopulatorFactory<FixedProcessingResultPopulator> fixedProcessingResultPopulatorFactory() {
        return build(builder(FixedProcessingResultPopulator.class)//
                .addConfigArg("result", ProcessingResult.class));
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
    MessagePopulatorFactory<FixedDurationValidityPeriodPopulator> fixedDurationValidityPeriodPopulatorFactory() {
        return build(builder(FixedDurationValidityPeriodPopulator.class)//
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
