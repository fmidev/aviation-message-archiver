package fi.fmi.avi.archiver.database;

import com.google.common.collect.ImmutableList;
import fi.fmi.avi.archiver.logging.model.FileProcessingStatistics;
import fi.fmi.avi.archiver.logging.model.LoggingContext;
import fi.fmi.avi.archiver.message.ArchivalStatus;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;

import java.util.List;

import static java.util.Objects.requireNonNull;

public class DatabaseService {

    private final DatabaseAccess databaseAccess;

    public DatabaseService(final DatabaseAccess databaseAccess) {
        this.databaseAccess = requireNonNull(databaseAccess, "databaseAccess");
    }

    public List<ArchiveAviationMessage> insertMessages(final List<ArchiveAviationMessage> messages, final LoggingContext loggingContext) {
        requireNonNull(messages, "messages");
        requireNonNull(loggingContext, "loggingContext");

        RuntimeException databaseInsertionException = null;
        final ImmutableList.Builder<ArchiveAviationMessage> updatedMessages = ImmutableList.builder();
        for (final ArchiveAviationMessage message : messages) {
            ArchivalStatus archivalStatus = ArchivalStatus.PENDING;
            try {
                loggingContext.enterBulletinMessage(message.getMessagePositionInFile());
                if (message.getProcessingResult() == fi.fmi.avi.archiver.message.ProcessingResult.OK) {
                    databaseAccess.insertAviationMessage(message, loggingContext);
                    archivalStatus = ArchivalStatus.ARCHIVED;
                    loggingContext.recordProcessingResult(FileProcessingStatistics.ProcessingResult.ARCHIVED);
                } else {
                    databaseAccess.insertRejectedAviationMessage(message, loggingContext);
                    archivalStatus = ArchivalStatus.REJECTED;
                    loggingContext.recordProcessingResult(FileProcessingStatistics.ProcessingResult.REJECTED);
                }
            } catch (final RuntimeException e) {
                databaseInsertionException = e;
                archivalStatus = ArchivalStatus.FAILED;
                loggingContext.recordProcessingResult(FileProcessingStatistics.ProcessingResult.FAILED);
            } finally {
                updatedMessages.add(message.toBuilder().setArchivalStatus(archivalStatus).build());
                loggingContext.leaveMessage();
            }
        }
        loggingContext.leaveBulletin();
        if (databaseInsertionException != null) {
            throw databaseInsertionException;
        }
        return updatedMessages.build();
    }

}
