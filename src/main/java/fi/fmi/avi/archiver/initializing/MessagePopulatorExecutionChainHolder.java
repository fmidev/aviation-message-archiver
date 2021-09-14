package fi.fmi.avi.archiver.initializing;

import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "message-populators")
public class MessagePopulatorExecutionChainHolder {
    private List<PopulatorInstanceSpec> executionChain = Collections.emptyList();

    public List<PopulatorInstanceSpec> getExecutionChain() {
        return executionChain;
    }

    public void setExecutionChain(final List<PopulatorInstanceSpec> executionChain) {
        this.executionChain = requireNonNull(executionChain, "executionChain");
    }

    public static class PopulatorInstanceSpec {
        private String name = "";
        private Map<String, Object> config = Collections.emptyMap();

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = requireNonNull(name, "name");
        }

        public Map<String, Object> getConfig() {
            return config;
        }

        public void setConfig(final Map<String, Object> config) {
            this.config = requireNonNull(config, "config");
        }

        @Override
        public String toString() {
            return "PopulatorInstanceSpec{" + "name='" + name + '\'' + ", config=" + config + '}';
        }
    }
}
