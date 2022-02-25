package fi.fmi.avi.archiver.logging;

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
                input.getFileProcessingIdentifier(), //
                input.getFileReference(), //
                input.getBulletinLogReference(), //
                input.getMessageLogReference());
    }
}
