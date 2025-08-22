package fi.fmi.avi.archiver.config;

import fi.fmi.avi.archiver.config.model.MessagePopulatorFactory;
import fi.fmi.avi.archiver.config.model.MessagePopulatorInstanceSpec;
import fi.fmi.avi.archiver.config.util.MessageProcessorsHelper;
import fi.fmi.avi.archiver.database.DatabaseAccess;
import fi.fmi.avi.archiver.message.processor.conditional.ConditionPropertyReader;
import fi.fmi.avi.archiver.message.processor.conditional.ConditionPropertyReaderFactory;
import fi.fmi.avi.archiver.message.processor.conditional.ConditionPropertyReaderRegistry;
import fi.fmi.avi.archiver.message.processor.conditional.RenamingConditionPropertyReaderFactory;
import fi.fmi.avi.archiver.message.processor.populator.ConditionalMessagePopulator;
import fi.fmi.avi.archiver.message.processor.populator.MessagePopulationService;
import fi.fmi.avi.archiver.message.processor.populator.MessagePopulator;
import fi.fmi.avi.archiver.message.processor.populator.StationIdPopulator;
import fi.fmi.avi.archiver.util.StringCaseFormat;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.List;
import java.util.stream.Stream;

@ConfigurationProperties(prefix = "production-line.message-populators")
public class MessagePopulatorConfig {
    @Bean(name = "messagePopulators")
    List<MessagePopulator> messagePopulators(
            final List<MessagePopulatorFactory<? extends MessagePopulator>> messagePopulatorFactories,
            final List<MessagePopulatorInstanceSpec> messagePopulatorSpecs,
            final DatabaseAccess databaseAccess,
            final MessageProcessorsHelper messageProcessorsHelper) {
        return Stream.concat(
                        messageProcessorsHelper.createMessageProcessors(
                                messagePopulatorFactories, messagePopulatorSpecs,
                                ConditionalMessagePopulator::new),
                        Stream.of(new StationIdPopulator(databaseAccess)))
                .toList();
    }

    @Bean
    ConditionPropertyReaderFactory conditionPropertyReaderFactory(final List<ConditionPropertyReader<?>> propertyReaders) {
        final ConditionPropertyReaderRegistry registry = new ConditionPropertyReaderRegistry();
        propertyReaders.forEach(registry::register);
        return new RenamingConditionPropertyReaderFactory(registry, StringCaseFormat::dashToCamel);
    }

    @Bean
    MessagePopulationService messagePopulationService(final List<MessagePopulator> messagePopulators) {
        return new MessagePopulationService(messagePopulators);
    }
}
