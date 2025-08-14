package fi.fmi.avi.archiver;

import fi.fmi.avi.archiver.logging.model.ReadableLoggingContext;

/**
 * Context for processing components.
 */
public interface ProcessingContext {
    ReadableLoggingContext getLoggingContext();
}
