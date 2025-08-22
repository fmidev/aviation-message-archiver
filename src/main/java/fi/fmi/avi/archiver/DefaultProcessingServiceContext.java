package fi.fmi.avi.archiver;

import fi.fmi.avi.archiver.logging.model.LoggingContext;

import static java.util.Objects.requireNonNull;

public class DefaultProcessingServiceContext implements ProcessingServiceContext {
    private final LoggingContext loggingContext;
    private volatile boolean processingFailures; // = false initially

    public DefaultProcessingServiceContext(final LoggingContext loggingContext) {
        this.loggingContext = requireNonNull(loggingContext, "loggingContext");
    }

    @Override
    public LoggingContext getLoggingContext() {
        return loggingContext;
    }

    @Override
    public boolean isProcessingErrors() {
        return processingFailures;
    }

    @Override
    public void signalProcessingErrors() {
        processingFailures = true;
    }
}
