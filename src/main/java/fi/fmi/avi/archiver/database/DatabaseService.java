package fi.fmi.avi.archiver.database;

import static java.util.Objects.requireNonNull;

import java.util.List;

import fi.fmi.avi.archiver.logging.FileProcessingStatistics;
import fi.fmi.avi.archiver.logging.LoggingContext;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;

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
                if (message.getProcessingResult() == fi.fmi.avi.archiver.message.ProcessingResult.OK) {
                    databaseAccess.insertAviationMessage(message, loggingContext);
                    loggingContext.recordProcessingResult(FileProcessingStatistics.ProcessingResult.ARCHIVED);
                } else {
                    databaseAccess.insertRejectedAviationMessage(message, loggingContext);
                    loggingContext.recordProcessingResult(FileProcessingStatistics.ProcessingResult.REJECTED);
                }
            } catch (final RuntimeException e) {
                databaseInsertionException = e;
                loggingContext.recordProcessingResult(FileProcessingStatistics.ProcessingResult.FAILED);
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
