package fi.fmi.avi.archiver.message;

import org.inferred.freebuilder.FreeBuilder;

@FreeBuilder
public abstract class ImmutableMessageProcessorContext implements MessageProcessorContext {
    ImmutableMessageProcessorContext() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public abstract Builder toBuilder();

    public static class Builder extends ImmutableMessageProcessorContext_Builder {
        Builder() {
        }
    }
}
