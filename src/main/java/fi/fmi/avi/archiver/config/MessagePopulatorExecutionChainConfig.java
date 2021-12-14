package fi.fmi.avi.archiver.config;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.context.annotation.Bean;

@ConstructorBinding
@ConfigurationProperties(prefix = "message-populators")
public class MessagePopulatorExecutionChainConfig {
    private final List<PopulatorInstanceSpec> executionChain;

    public MessagePopulatorExecutionChainConfig(final List<PopulatorInstanceSpec> executionChain) {
        this.executionChain = requireNonNull(executionChain, "executionChain");
    }

    @Bean
    public List<PopulatorInstanceSpec> executionChain() {
        return Collections.unmodifiableList(executionChain);
    }

    public static class PopulatorInstanceSpec {
        private final String name;
        private final Map<String, Object> config;

        public PopulatorInstanceSpec(final String name, final Map<String, Object> config) {
            this.name = requireNonNull(name, "name");
            checkArgument(!name.isEmpty(), "populator name cannot be empty");
            this.config = config != null ? config : Collections.emptyMap();
        }

        public String getName() {
            return name;
        }

        public Map<String, Object> getConfig() {
            return config;
        }

        @Override
        public String toString() {
            return "PopulatorInstanceSpec{" + "name='" + name + '\'' + ", config=" + config + '}';
        }
    }
}
