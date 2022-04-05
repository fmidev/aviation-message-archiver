package fi.fmi.avi.archiver.config;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;

import com.google.common.collect.ImmutableList;

import fi.fmi.avi.archiver.config.model.MessagePopulatorFactory;
import fi.fmi.avi.archiver.config.model.PopulatorInstanceSpec;
import fi.fmi.avi.archiver.database.DatabaseAccess;
import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.logging.model.LoggingContext;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.populator.MessagePopulationService;
import fi.fmi.avi.archiver.message.populator.MessagePopulator;
import fi.fmi.avi.archiver.message.populator.StationIdPopulator;
import fi.fmi.avi.archiver.message.populator.conditional.ActivationCondition;
import fi.fmi.avi.archiver.message.populator.conditional.ConditionPropertyReader;
import fi.fmi.avi.archiver.message.populator.conditional.ConditionPropertyReaderFactory;
import fi.fmi.avi.archiver.message.populator.conditional.ConditionPropertyReaderRegistry;
import fi.fmi.avi.archiver.message.populator.conditional.ConditionalMessagePopulator;
import fi.fmi.avi.archiver.message.populator.conditional.GeneralPropertyPredicate;
import fi.fmi.avi.archiver.message.populator.conditional.PropertyActivationCondition;
import fi.fmi.avi.archiver.message.populator.conditional.RenamingConditionPropertyReaderFactory;
import fi.fmi.avi.archiver.util.StringCaseFormat;
import fi.fmi.avi.archiver.util.instantiation.ConfigValueConverter;

@ConfigurationProperties(prefix = "message-populators")
public class MessagePopulatorConfig {

    private final ImmutableList<PopulatorInstanceSpec> executionChain;

    @ConstructorBinding
    MessagePopulatorConfig(final PopulatorInstanceSpec.Builder[] executionChain) {
        this(buildAll(requireNonNull(executionChain, "executionChain")));
    }

    MessagePopulatorConfig(final List<PopulatorInstanceSpec> executionChain) {
        requireNonNull(executionChain, "executionChain");
        checkState(!executionChain.isEmpty(), "Invalid message populators configuration: executionChain is empty");
        this.executionChain = ImmutableList.copyOf(executionChain);
    }

    private static List<PopulatorInstanceSpec> buildAll(final PopulatorInstanceSpec.Builder[] builders) {
        final ImmutableList.Builder<PopulatorInstanceSpec> specs = ImmutableList.builder();
        for (int i = 0; i < builders.length; i++) {
            try {
                specs.add(builders[i].build());
            } catch (final RuntimeException e) {
                throw new IllegalStateException("Invalid MessagePopulator specification at index <" + i + ">: " + e.getMessage(), e);
            }
        }
        return specs.build();
    }

    @Bean
    List<PopulatorInstanceSpec> executionChain() {
        return executionChain;
    }

    @Bean(name = "messagePopulators")
    List<MessagePopulator> messagePopulators(final List<MessagePopulatorFactory<?>> messagePopulatorFactories, final List<PopulatorInstanceSpec> executionChain,
            final DatabaseAccess databaseAccess, final ConfigValueConverter messagePopulatorConfigValueConverter,
            final ConditionPropertyReaderFactory conditionPropertyReaderFactory) {
        final Map<String, MessagePopulatorFactory<?>> factoriesByName = messagePopulatorFactories.stream()//
                .collect(Collectors.toMap(MessagePopulatorFactory::getName, Function.identity()));
        final ImmutableList.Builder<MessagePopulator> populatorsBuilder = executionChain.stream()//
                .map(spec -> createMessagePopulator(spec, factoriesByName, messagePopulatorConfigValueConverter, conditionPropertyReaderFactory))//
                .collect(ImmutableList::builder, ImmutableList.Builder::add, (builder1, builder2) -> builder1.addAll(builder2.build()));
        populatorsBuilder.add(new StationIdPopulator(databaseAccess));
        return populatorsBuilder.build();
    }

    private MessagePopulator createMessagePopulator(final PopulatorInstanceSpec spec, final Map<String, MessagePopulatorFactory<?>> populatorFactoriesByName,
            final ConfigValueConverter converter, final ConditionPropertyReaderFactory conditionPropertyReaderFactory) {
        final MessagePopulator messagePopulator = populatorFactoriesByName.get(spec.getName()).newInstance(spec.getConfig());
        return applyMessagePopulatorActivationCondition(messagePopulator, spec, converter, conditionPropertyReaderFactory);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private MessagePopulator applyMessagePopulatorActivationCondition(final MessagePopulator messagePopulator, final PopulatorInstanceSpec spec,
            final ConfigValueConverter converter, final ConditionPropertyReaderFactory conditionPropertyReaderFactory) {
        return spec.getActivateOn().entrySet().stream()//
                .map(entry -> {
                    try {
                        final String propertyName = entry.getKey();
                        final ConditionPropertyReader conditionPropertyReader = conditionPropertyReaderFactory.getInstance(propertyName);
                        final GeneralPropertyPredicate<?> propertyPredicate = entry.getValue()
                                .transform(element -> converter.toReturnValueType(element, conditionPropertyReader.getValueGetterForType()))//
                                .validate(conditionPropertyReader::validate)//
                                .build();
                        return new PropertyActivationCondition(conditionPropertyReader, propertyPredicate);
                    } catch (final RuntimeException e) {
                        throw new IllegalStateException("Unable to initialize '" + spec.getName() + "' activateOn: " + e.getMessage(), e);
                    }
                })//
                .collect(Collectors.collectingAndThen(Collectors.toList(), ActivationCondition::and))//
                .<MessagePopulator> map(activationCondition -> new ConditionalMessagePopulator(activationCondition, messagePopulator))//
                .orElse(messagePopulator);
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

        public Message<List<ArchiveAviationMessage>> populateMessages(final List<InputAviationMessage> inputMessages, final MessageHeaders headers) {
            requireNonNull(inputMessages, "inputMessages");
            requireNonNull(headers, "headers");

            final LoggingContext loggingContext = SpringLoggingContextHelper.getLoggingContext(headers);
            final List<MessagePopulationService.PopulationResult> populationResults = messagePopulationService.populateMessages(inputMessages, loggingContext);

            final List<ArchiveAviationMessage> populatedMessages = new ArrayList<>();
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
                            populatedMessages.add(archiveMessage);
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
