package fi.fmi.avi.archiver.config;

import fi.fmi.avi.archiver.config.model.PopulatorInstanceSpec;
import fi.fmi.avi.archiver.database.DatabaseAccess;
import fi.fmi.avi.archiver.message.populator.MessagePopulator;
import fi.fmi.avi.archiver.message.populator.MessagePopulatorFactory;
import fi.fmi.avi.archiver.message.populator.StationIdPopulator;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

@ConstructorBinding
@ConfigurationProperties(prefix = "message-populators")
public class MessagePopulatorConfig {

    private final List<PopulatorInstanceSpec> executionChain;

    MessagePopulatorConfig(final List<PopulatorInstanceSpec> executionChain) {
        this.executionChain = requireNonNull(executionChain, "executionChain");
        checkState(!executionChain.isEmpty(), "Message populator execution chain not configured");
    }

    @Bean
    List<PopulatorInstanceSpec> executionChain() {
        return Collections.unmodifiableList(executionChain);
    }

    @Bean(name = "messagePopulators")
    public List<MessagePopulator> messagePopulators(final List<MessagePopulatorFactory<?>> messagePopulatorFactories,
                                                    final List<PopulatorInstanceSpec> executionChain,
                                                    final DatabaseAccess databaseAccess) {
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
