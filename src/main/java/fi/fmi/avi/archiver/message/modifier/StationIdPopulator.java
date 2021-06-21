package fi.fmi.avi.archiver.message.modifier;

import fi.fmi.avi.archiver.database.DatabaseAccess;
import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

public class StationIdPopulator implements MessagePopulator {

    private final DatabaseAccess databaseAccess;

    public StationIdPopulator(final DatabaseAccess databaseAccess) {
        this.databaseAccess = requireNonNull(databaseAccess, "databaseAccess");
    }

    @Override
    public void modify(InputAviationMessage inputAviationMessage, ArchiveAviationMessage.Builder aviationMessageBuilder) {
        final Optional<Integer> stationId = databaseAccess.queryStationId(aviationMessageBuilder.getIcaoAirportCode());
        stationId.ifPresent(aviationMessageBuilder::setStationId);
    }

}
