package fi.fmi.avi.archiver.logging.model;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class ImmutableLoggingContext extends AbstractLoggingContext {
    ImmutableLoggingContext() {
    }

    public static ImmutableLoggingContext copyOf(final ReadableLoggingContext input) {
        if (input instanceof ImmutableLoggingContext) {
            return (ImmutableLoggingContext) input;
        }
        return new AutoValue_ImmutableLoggingContext(//
                input.getProcessingId(), //
                input.getFile(), //
                input.getBulletin(), //
                input.getMessage());
    }
}
