package fi.fmi.avi.archiver.database;

import static java.util.Objects.requireNonNull;

import java.util.List;

import fi.fmi.avi.archiver.logging.FileProcessingStatistics;
import fi.fmi.avi.archiver.logging.LoggingContext;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.ProcessingResult;

public class DatabaseService {

    private final DatabaseAccess databaseAccess;

    public DatabaseService(final DatabaseAccess databaseAccess) {
        this.databaseAccess = requireNonNull(databaseAccess, "databaseAccess");
    }

    public List<ArchiveAviationMessage> insertMessages(final List<ArchiveAviationMessage> messages, final LoggingContext loggingContext) {
        requireNonNull(messages, "messages");
        requireNonNull(loggingContext, "loggingContext");

        RuntimeException databaseInsertionException = null;
        for (final ArchiveAviationMessage message : messages) {
            try {
                loggingContext.enterMessage(message.getMessagePositionInFile());
                if (message.getProcessingResult() == ProcessingResult.OK) {
                    databaseAccess.insertAviationMessage(message, loggingContext);
                    loggingContext.recordStatus(FileProcessingStatistics.Status.ARCHIVED);
                } else {
                    databaseAccess.insertRejectedAviationMessage(message, loggingContext);
                    loggingContext.recordStatus(FileProcessingStatistics.Status.REJECTED);
                }
            } catch (final RuntimeException e) {
                databaseInsertionException = e;
                loggingContext.recordStatus(FileProcessingStatistics.Status.FAILED);
            } finally {
                loggingContext.leaveMessage();
            }
        }
        loggingContext.leaveBulletin();
        if (databaseInsertionException != null) {
            throw databaseInsertionException;
        }
        return messages;
    }

}
