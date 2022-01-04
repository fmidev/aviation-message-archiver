package fi.fmi.avi.archiver.logging;

import static java.util.Objects.requireNonNull;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.fmi.avi.archiver.file.FileReference;

public class Slf4jProcessingChainLogger implements ProcessingChainLogger {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessingChainLogger.class);

    private final LoggingContext loggingContext;
    private final FileProcessingStatistics fileProcessingStatistics;

    public Slf4jProcessingChainLogger(final LoggingContext loggingContext, final FileProcessingStatistics fileProcessingStatistics) {
        this.loggingContext = requireNonNull(loggingContext, "loggingContext");
        this.fileProcessingStatistics = requireNonNull(fileProcessingStatistics, "fileProcessingStatistics");
    }

    @Override
    public LoggingContext getContext() {
        return loggingContext;
    }

    @Override
    public FileProcessingStatistics getStatistics() {
        return fileProcessingStatistics;
    }

    @Override
    public void logStart(@Nullable final FileReference fileReference) {
        getContext().enterFile(fileReference);
        LOGGER.info("Start processing <{}>", getContext());
    }

    @Override
    public void logError(@Nullable final Object message, @Nullable final Throwable throwable) {
        LOGGER.error("Error while processing <{}>: {}", getContext(), message, throwable);
        collectContextStatistics(FileProcessingStatistics.Status.FAILED);
    }

    @Override
    public void logFinish(final boolean withErrors) {
        getContext().leaveBulletin();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Finish processing <{}> {}. Statistics: {}", getContext(), withErrors ? "with errors" : "successfully", getStatistics());
        }
    }
}
