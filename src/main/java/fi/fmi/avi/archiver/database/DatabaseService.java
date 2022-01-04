package fi.fmi.avi.archiver.database;

import static java.util.Objects.requireNonNull;

import java.util.List;

import fi.fmi.avi.archiver.logging.FileProcessingStatistics;
import fi.fmi.avi.archiver.logging.ProcessingChainLogger;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.ProcessingResult;

public class DatabaseService {

    private final DatabaseAccess databaseAccess;

    public DatabaseService(final DatabaseAccess databaseAccess) {
        this.databaseAccess = requireNonNull(databaseAccess, "databaseAccess");
    }

    public List<ArchiveAviationMessage> insertMessages(final List<ArchiveAviationMessage> messages, final ProcessingChainLogger logger) {
        RuntimeException databaseInsertionException = null;
        for (final ArchiveAviationMessage message : messages) {
            try {
                logger.getContext().enterMessage(message.getMessageReference());
                if (message.getProcessingResult() == ProcessingResult.OK) {
                    databaseAccess.insertAviationMessage(message, logger.getContext());
                    logger.collectContextStatistics(FileProcessingStatistics.Status.ARCHIVED);
                } else {
                    databaseAccess.insertRejectedAviationMessage(message, logger.getContext());
                    logger.collectContextStatistics(FileProcessingStatistics.Status.REJECTED);
                }
            } catch (final RuntimeException e) {
                databaseInsertionException = e;
                logger.collectContextStatistics(FileProcessingStatistics.Status.FAILED);
            } finally {
                logger.getContext().leaveMessage();
            }
        }
        logger.getContext().leaveBulletin();
        if (databaseInsertionException != null) {
            throw databaseInsertionException;
        }
        return messages;
    }

}
