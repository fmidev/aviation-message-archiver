package fi.fmi.avi.archiver.config.model;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.Map;

import javax.annotation.Nullable;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fi.fmi.avi.archiver.message.populator.conditional.GeneralPropertyPredicate;

@SuppressWarnings("AssignmentOrReturnOfFieldWithMutableType")
@SuppressFBWarnings("EI_EXPOSE_REP")
public class PopulatorInstanceSpec {

    private final String name;
    private final Map<String, GeneralPropertyPredicate.Builder<?>> activateOn;
    private final Map<String, Object> config;

    public PopulatorInstanceSpec(final String name, @Nullable final Map<String, GeneralPropertyPredicate.Builder<?>> activateOn,
            @Nullable final Map<String, Object> config) {
        this.name = requireNonNull(name, "name");
        this.activateOn = activateOn == null ? Collections.emptyMap() : activateOn;
        this.config = config == null ? Collections.emptyMap() : config;
        checkArgument(!name.isEmpty(), "populator name cannot be empty");
    }

    public String getName() {
        return name;
    }

    public Map<String, GeneralPropertyPredicate.Builder<?>> getActivateOn() {
        return activateOn;
    }

    public Map<String, Object> getConfig() {
        return config;
    }

    @Override
    public String toString() {
        return "PopulatorInstanceSpec{" + "name='" + name + '\'' + ", activateOn=" + activateOn + ", config=" + config + '}';
    }

}
