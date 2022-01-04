package fi.fmi.avi.archiver.logging;

import static java.util.Objects.requireNonNull;

import javax.annotation.Nullable;

import fi.fmi.avi.archiver.file.FileMetadata;
import fi.fmi.avi.archiver.file.FileReference;
import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;

public final class NoOpProcessingChainLogger implements ProcessingChainLogger {
    private static final NoOpProcessingChainLogger INSTANCE = new NoOpProcessingChainLogger();

    private NoOpProcessingChainLogger() {
    }

    public static NoOpProcessingChainLogger getInstance() {
        return INSTANCE;
    }

    @Override
    public void logStart(@Nullable final FileMetadata fileMetadata) {
    }

    @Override
    public void logStart(@Nullable final FileReference fileReference) {
    }

    @Override
    public void logError(@Nullable final Throwable throwable) {
    }

    @Override
    public void logError(@Nullable final Object message, @Nullable final Throwable throwable) {
    }

    @Override
    public void logFinish(final boolean withErrors) {
    }

    @Override
    public void collectContextStatistics(final FileProcessingStatistics.Status status) {
        requireNonNull(status, "status");
    }

    public void enterMessage(final InputAviationMessage message) {
        requireNonNull(message, "message");
    }

    public void enterMessage(final ArchiveAviationMessage message) {
        requireNonNull(message, "message");
    }

    @Override
    public LoggingContext getContext() {
        return NoOpLoggingContext.getInstance();
    }

    @Override
    public FileProcessingStatistics getStatistics() {
        return NoOpFileProcessingStatistics.getInstance();
    }
}
