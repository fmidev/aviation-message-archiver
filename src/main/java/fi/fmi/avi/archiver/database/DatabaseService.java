package fi.fmi.avi.archiver.database;

import com.google.common.collect.ImmutableList;
import fi.fmi.avi.archiver.ProcessingServiceContext;
import fi.fmi.avi.archiver.logging.model.FileProcessingStatistics;
import fi.fmi.avi.archiver.logging.model.LoggingContext;
import fi.fmi.avi.archiver.message.ArchivalStatus;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.InputAndArchiveAviationMessage;

import java.util.List;

import static java.util.Objects.requireNonNull;

public class DatabaseService {

    private final DatabaseAccess databaseAccess;

    public DatabaseService(final DatabaseAccess databaseAccess) {
        this.databaseAccess = requireNonNull(databaseAccess, "databaseAccess");
    }

    public List<InputAndArchiveAviationMessage> insertMessages(final List<InputAndArchiveAviationMessage> messages, final ProcessingServiceContext context) {
        requireNonNull(messages, "messages");
        requireNonNull(context, "context");

        final LoggingContext loggingContext = context.getLoggingContext();
        RuntimeException databaseInsertionException = null;
        final ImmutableList.Builder<InputAndArchiveAviationMessage> updatedMessages = ImmutableList.builder();
        for (final InputAndArchiveAviationMessage inputAndArchiveMessage : messages) {
            final ArchiveAviationMessage message = inputAndArchiveMessage.archiveMessage();
            ArchivalStatus archivalStatus = message.getArchivalStatus();
            try {
                loggingContext.enterBulletinMessage(inputAndArchiveMessage.inputMessage().getMessagePositionInFile());
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
                context.signalProcessingErrors();
                loggingContext.recordProcessingResult(FileProcessingStatistics.ProcessingResult.FAILED);
            } finally {
                updatedMessages.add(inputAndArchiveMessage.withArchiveMessage(message.toBuilder().setArchivalStatus(archivalStatus).build()));
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
