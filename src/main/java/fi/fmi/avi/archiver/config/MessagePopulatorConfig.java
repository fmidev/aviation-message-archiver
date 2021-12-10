package fi.fmi.avi.archiver.config;

import fi.fmi.avi.archiver.database.DatabaseAccess;
import fi.fmi.avi.archiver.initializing.MessagePopulatorExecutionChainHolder;
import fi.fmi.avi.archiver.message.populator.MessagePopulator;
import fi.fmi.avi.archiver.message.populator.MessagePopulatorFactory;
import fi.fmi.avi.archiver.message.populator.StationIdPopulator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Configuration
public class MessagePopulatorConfig {

    @Autowired
    private List<MessagePopulatorFactory<?>> messagePopulatorFactories;

    @Autowired
    private MessagePopulatorExecutionChainHolder messagePopulatorExecutionChainHolder;

    @Autowired
    private DatabaseAccess databaseAccess;

    @Bean(name = "messagePopulators")
    public List<MessagePopulator> messagePopulators() {
        final Map<String, MessagePopulatorFactory<?>> factoriesByName = messagePopulatorFactories.stream()//
                .collect(Collectors.toMap(MessagePopulatorFactory::getName, Function.identity()));
        final ArrayList<MessagePopulator> populators = messagePopulatorExecutionChainHolder.getExecutionChain().stream()//
                .map(spec -> factoriesByName.get(spec.getName()).newInstance(spec.getConfig()))//
                .collect(Collectors.toCollection(ArrayList::new));
        populators.add(new StationIdPopulator(databaseAccess));
        populators.trimToSize();
        return Collections.unmodifiableList(populators);
    }
}
