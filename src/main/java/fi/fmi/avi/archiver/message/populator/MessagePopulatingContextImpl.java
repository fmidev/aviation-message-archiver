package fi.fmi.avi.archiver.message.populator;

import org.inferred.freebuilder.FreeBuilder;

@FreeBuilder
public abstract class MessagePopulatingContextImpl implements MessagePopulatingContext {
    public static Builder builder() {
        return new Builder();
    }

    public abstract Builder toBuilder();

    public static class Builder extends MessagePopulatingContextImpl_Builder {
        Builder() {
        }
    }
}
