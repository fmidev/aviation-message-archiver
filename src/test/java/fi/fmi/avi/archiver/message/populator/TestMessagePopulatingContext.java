package fi.fmi.avi.archiver.message.populator;

import com.google.auto.value.AutoValue;

import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.logging.model.NoOpLoggingContext;
import fi.fmi.avi.archiver.logging.model.ReadableLoggingContext;

@AutoValue
public abstract class TestMessagePopulatingContext implements MessagePopulatingContext {
    TestMessagePopulatingContext() {
    }

    public static TestMessagePopulatingContext create(final InputAviationMessage inputAviationMessage) {
        return new AutoValue_TestMessagePopulatingContext(inputAviationMessage);
    }

    @Override
    public ReadableLoggingContext getLoggingContext() {
        return NoOpLoggingContext.getInstance();
    }
}
