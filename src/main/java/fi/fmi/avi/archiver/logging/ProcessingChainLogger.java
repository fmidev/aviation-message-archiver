package fi.fmi.avi.archiver.logging;

import static java.util.Objects.requireNonNull;

import javax.annotation.Nullable;

import fi.fmi.avi.archiver.file.FileMetadata;
import fi.fmi.avi.archiver.file.FileReference;

public interface ProcessingChainLogger {
    LoggingContext getContext();

    FileProcessingStatistics getStatistics();

    default void collectContextStatistics(final FileProcessingStatistics.Status status) {
        requireNonNull(status, "status");
        final LoggingContext context = getContext();
        final int bulletinIndex = context.getBulletinIndex();
        if (bulletinIndex < 0) {
            getStatistics().recordFileStatus(status);
        } else {
            final int messageIndex = context.getMessageIndex();
            if (messageIndex < 0) {
                getStatistics().recordBulletinStatus(bulletinIndex, status);
                getContext().leaveBulletin();
            } else {
                getStatistics().recordMessageStatus(bulletinIndex, messageIndex, status);
                getContext().leaveMessage();
            }
        }
    }

    default void logStart(@Nullable final FileMetadata fileMetadata) {
        logStart(fileMetadata == null ? null : fileMetadata.getFileReference());
    }

    void logStart(@Nullable FileReference fileReference);

    default void logError(@Nullable final Throwable throwable) {
        logError(String.valueOf(throwable), throwable);
    }

    void logError(@Nullable final Object message, @Nullable Throwable throwable);

    void logFinish(boolean withErrors);
}
