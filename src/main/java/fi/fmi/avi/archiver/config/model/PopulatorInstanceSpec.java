package fi.fmi.avi.archiver.config.model;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.Map;

import javax.annotation.Nullable;

import org.inferred.freebuilder.FreeBuilder;

import fi.fmi.avi.archiver.message.populator.conditional.GeneralPropertyPredicate;

@FreeBuilder
public abstract class PopulatorInstanceSpec {
    PopulatorInstanceSpec() {
    }

    public abstract String getName();

    public abstract Map<String, GeneralPropertyPredicate.Builder<?>> getActivateOn();

    public abstract Map<String, Object> getConfig();

    public static class Builder extends PopulatorInstanceSpec_Builder {
        Builder() {
        }

        @Override
        public Builder setName(final String name) {
            requireNonNull(name);
            checkArgument(!name.isEmpty(), "populator name cannot be empty");
            return super.setName(name);
        }

        public Builder setActivateOn(@Nullable final Map<? extends String, ? extends GeneralPropertyPredicate.Builder<?>> map) {
            return clearActivateOn()//
                    .putAllActivateOn(map == null ? Collections.emptyMap() : map);
        }

        public Builder setConfig(@Nullable final Map<? extends String, ?> map) {
            return clearConfig()//
                    .putAllConfig(map == null ? Collections.emptyMap() : map);
        }
    }
}
