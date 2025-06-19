package fi.fmi.avi.archiver.config.model;

import fi.fmi.avi.archiver.message.processor.conditional.GeneralPropertyPredicate;
import org.inferred.freebuilder.FreeBuilder;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

@FreeBuilder
public abstract class MessagePopulatorInstanceSpec implements MessageProcessorInstanceSpec {
    MessagePopulatorInstanceSpec() {
    }

    public static class Builder extends MessagePopulatorInstanceSpec_Builder {
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
