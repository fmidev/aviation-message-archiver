package fi.fmi.avi.archiver.database;

import fi.fmi.avi.archiver.message.AviationMessage;
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
    public void insertMessages(final List<AviationMessage> messages) {
        for (final AviationMessage message : messages) {
            if (message.getProcessingResult() == ProcessingResult.OK) {
                databaseAccess.insertAviationMessage(message);
            } else {
                databaseAccess.insertRejectedAviationMessage(message);
            }
        }
    }

}
