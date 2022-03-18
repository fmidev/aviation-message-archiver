package fi.fmi.avi.archiver.message.populator;

import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.logging.model.ReadableLoggingContext;

public interface MessagePopulatingContext {
    ReadableLoggingContext getLoggingContext();

    InputAviationMessage getInputMessage();
}
