package fi.fmi.avi.archiver.message;

import com.google.auto.value.AutoValue;
import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.logging.model.NoOpLoggingContext;
import fi.fmi.avi.archiver.logging.model.ReadableLoggingContext;

@AutoValue
public abstract class TestMessageProcessorContext implements MessageProcessorContext {
    TestMessageProcessorContext() {
    }

    public static TestMessageProcessorContext create(final InputAviationMessage inputAviationMessage) {
        return new AutoValue_TestMessageProcessorContext(inputAviationMessage);
    }

    @Override
    public ReadableLoggingContext getLoggingContext() {
        return NoOpLoggingContext.getInstance();
    }
}
