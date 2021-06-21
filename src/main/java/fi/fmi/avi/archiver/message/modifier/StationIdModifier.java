package fi.fmi.avi.archiver.message.modifier;

import fi.fmi.avi.archiver.database.DatabaseAccess;
import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

public class StationIdModifier implements MessageModifier {

    private final DatabaseAccess databaseAccess;

    public StationIdModifier(final DatabaseAccess databaseAccess) {
        this.databaseAccess = requireNonNull(databaseAccess, "databaseAccess");
    }

    @Override
    public void modify(InputAviationMessage InputAviationMessage, ArchiveAviationMessage.Builder aviationMessageBuilder) {
        final Optional<Integer> stationId = databaseAccess.queryStationId(aviationMessageBuilder.getIcaoAirportCode());
        stationId.ifPresent(aviationMessageBuilder::setStationId);
    }

}
