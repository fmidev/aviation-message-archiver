package fi.fmi.avi.archiver.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fi.fmi.avi.archiver.database.DatabaseAccess;
import fi.fmi.avi.archiver.message.populator.MessagePopulator;
import fi.fmi.avi.archiver.message.populator.MessagePopulatorFactory;
import fi.fmi.avi.archiver.message.populator.StationIdPopulator;

@Configuration
public class MessagePopulatorConfig {

    @Bean(name = "messagePopulators")
    public List<MessagePopulator> messagePopulators(final List<MessagePopulatorFactory<?>> messagePopulatorFactories,
            final List<MessagePopulatorExecutionChainConfig.PopulatorInstanceSpec> executionChain, final DatabaseAccess databaseAccess) {
        final Map<String, MessagePopulatorFactory<?>> factoriesByName = messagePopulatorFactories.stream()//
                .collect(Collectors.toMap(MessagePopulatorFactory::getName, Function.identity()));
        final ArrayList<MessagePopulator> populators = executionChain.stream()//
                .map(spec -> factoriesByName.get(spec.getName()).newInstance(spec.getConfig()))//
                .collect(Collectors.toCollection(ArrayList::new));
        populators.add(new StationIdPopulator(databaseAccess));
        populators.trimToSize();
        return Collections.unmodifiableList(populators);
    }

}
