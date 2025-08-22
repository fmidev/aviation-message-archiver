package fi.fmi.avi.archiver;

import fi.fmi.avi.archiver.logging.model.LoggingContext;

/**
 * Context for message processing services.
 */
public interface ProcessingServiceContext extends ProcessingContext {
    @Override
    LoggingContext getLoggingContext();

    /**
     * Return whether processing has {@link #signalProcessingErrors() signalled} any processing errors.
     *
     * @return a boolean denoting whether processing has signalled any processing errors
     */
    boolean isProcessingErrors();

    /**
     * Signal a processing error.
     * After this method has been invoked at least once, {@link #isProcessingErrors()} will return {@code true}.
     */
    void signalProcessingErrors();
}
