package fi.fmi.avi.archiver.initializing;

import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@SuppressWarnings("AssignmentOrReturnOfFieldWithMutableType")
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

    @SuppressWarnings("AssignmentOrReturnOfFieldWithMutableType")
    public static class PopulatorInstanceSpec {
        private String name = "";
        private Map<String, Object> arguments = Collections.emptyMap();
        private Map<String, Object> options = Collections.emptyMap();

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = requireNonNull(name, "name");
        }

        public Map<String, Object> getArguments() {
            return arguments;
        }

        public void setArguments(final Map<String, Object> arguments) {
            this.arguments = requireNonNull(arguments, "arguments");
        }

        public Map<String, Object> getOptions() {
            return options;
        }

        public void setOptions(final Map<String, Object> options) {
            this.options = requireNonNull(options, "config");
        }

        @Override
        public String toString() {
            return "PopulatorInstanceSpec{" + "name='" + name + '\'' + ", arguments=" + arguments + ", options=" + options + '}';
        }
    }
}
