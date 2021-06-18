package fi.fmi.avi.archiver.database;

import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.ProcessingResult;
import org.springframework.integration.annotation.ServiceActivator;

import java.util.List;

import static java.util.Objects.requireNonNull;

public class DatabaseService {

    private final DatabaseAccess databaseAccess;

    public DatabaseService(final DatabaseAccess databaseAccess) {
        this.databaseAccess = requireNonNull(databaseAccess, "databaseAccess");
    }

    @ServiceActivator
    public List<ArchiveAviationMessage> insertMessages(final List<ArchiveAviationMessage> messages) {
        RuntimeException databaseInsertionException = null;
        for (final ArchiveAviationMessage message : messages) {
            try {
                if (message.getProcessingResult() == ProcessingResult.OK) {
                    databaseAccess.insertAviationMessage(message);
                } else {
                    databaseAccess.insertRejectedAviationMessage(message);
                }
            } catch (final RuntimeException e) {
                databaseInsertionException = e;
            }
        }
        if (databaseInsertionException != null) {
            throw databaseInsertionException;
        }
        return messages;
    }

}
