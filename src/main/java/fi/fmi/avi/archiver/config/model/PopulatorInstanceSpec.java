package fi.fmi.avi.archiver.config.model;

import java.util.Collections;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

public class PopulatorInstanceSpec {

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
