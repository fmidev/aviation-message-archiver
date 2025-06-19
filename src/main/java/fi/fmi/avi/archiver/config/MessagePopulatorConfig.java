package fi.fmi.avi.archiver.config;

import fi.fmi.avi.archiver.config.model.MessagePopulatorFactory;
import fi.fmi.avi.archiver.config.model.MessagePopulatorInstanceSpec;
import fi.fmi.avi.archiver.config.util.MessageProcessorsHelper;
import fi.fmi.avi.archiver.config.util.SpringLoggingContextHelper;
import fi.fmi.avi.archiver.database.DatabaseAccess;
import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.logging.model.LoggingContext;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.InputAndArchiveAviationMessage;
import fi.fmi.avi.archiver.message.processor.conditional.ConditionPropertyReader;
import fi.fmi.avi.archiver.message.processor.conditional.ConditionPropertyReaderFactory;
import fi.fmi.avi.archiver.message.processor.conditional.ConditionPropertyReaderRegistry;
import fi.fmi.avi.archiver.message.processor.conditional.RenamingConditionPropertyReaderFactory;
import fi.fmi.avi.archiver.message.processor.populator.ConditionalMessagePopulator;
import fi.fmi.avi.archiver.message.processor.populator.MessagePopulationService;
import fi.fmi.avi.archiver.message.processor.populator.MessagePopulator;
import fi.fmi.avi.archiver.message.processor.populator.StationIdPopulator;
import fi.fmi.avi.archiver.util.StringCaseFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

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

    @Bean
    MessagePopulationIntegrationService messagePopulationIntegrationService(final MessagePopulationService messagePopulationService) {
        return new MessagePopulationIntegrationService(messagePopulationService);
    }

    public static class MessagePopulationIntegrationService {
        private static final Logger LOGGER = LoggerFactory.getLogger(MessagePopulationIntegrationService.class);

        private final MessagePopulationService messagePopulationService;

        public MessagePopulationIntegrationService(final MessagePopulationService messagePopulationService) {
            this.messagePopulationService = requireNonNull(messagePopulationService, "messagePopulationService");
        }

        public Message<List<InputAndArchiveAviationMessage>> populateMessages(final List<InputAviationMessage> inputMessages, final MessageHeaders headers) {
            requireNonNull(inputMessages, "inputMessages");
            requireNonNull(headers, "headers");

            final LoggingContext loggingContext = SpringLoggingContextHelper.getLoggingContext(headers);
            final List<MessagePopulationService.PopulationResult> populationResults = messagePopulationService.populateMessages(inputMessages, loggingContext);

            final List<InputAndArchiveAviationMessage> populatedMessages = new ArrayList<>();
            boolean failures = false;
            for (final MessagePopulationService.PopulationResult populationResult : populationResults) {
                loggingContext.enterBulletinMessage(populationResult.getInputMessage().getMessagePositionInFile());
                switch (populationResult.getStatus()) {
                    case STORE:
                        final ArchiveAviationMessage archiveMessage = populationResult.getArchiveMessage().orElse(null);
                        if (archiveMessage == null) {
                            LOGGER.error("PopulationResult of status {} is missing message in <{}>.", MessagePopulationService.PopulationResult.Status.STORE,
                                    loggingContext);
                        } else {
                            final InputAviationMessage inputMessage = populationResult.getInputMessage();
                            populatedMessages.add(new InputAndArchiveAviationMessage(inputMessage, archiveMessage));
                        }
                        break;
                    case DISCARD:
                        break;
                    case FAIL:
                        failures = true;
                        break;
                    default:
                        LOGGER.error("Unknown PopulationResult status '{}' in <{}>.", populationResult.getStatus(), loggingContext);
                }
            }
            loggingContext.leaveBulletin();

            return MessageBuilder.withPayload(Collections.unmodifiableList(populatedMessages))//
                    .copyHeaders(headers)//
                    .setHeader(IntegrationFlowConfig.PROCESSING_ERRORS.getName(), IntegrationFlowConfig.hasProcessingErrors(headers) || failures)//
                    .build();
        }
    }
}
