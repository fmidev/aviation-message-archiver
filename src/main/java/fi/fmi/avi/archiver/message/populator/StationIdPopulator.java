package fi.fmi.avi.archiver.message.populator;

import static java.util.Objects.requireNonNull;

import java.util.Optional;
import java.util.OptionalInt;

import fi.fmi.avi.archiver.database.DatabaseAccess;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.ProcessingResult;

/**
 * Resolves database id for resolved {@link ArchiveAviationMessage#getStationIcaoCode() station ICAO code} and populates the corresponding
 * {@link ArchiveAviationMessage#getStationId() numeric id}.
 *
 * <p>
 * This populator is always implicitly added in the end of populator execution chain, and should be omitted from execution chain configuration.
 * </p>
 */
public class StationIdPopulator implements MessagePopulator {

    private final DatabaseAccess databaseAccess;

    public StationIdPopulator(final DatabaseAccess databaseAccess) {
        this.databaseAccess = requireNonNull(databaseAccess, "databaseAccess");
    }

    @Override
    public void populate(final MessagePopulatingContext context, final ArchiveAviationMessage.Builder target) {
        requireNonNull(context, "context");
        requireNonNull(target, "target");

        final Optional<Integer> stationId = databaseAccess.queryStationId(target.getStationIcaoCode(), context.getLoggingContext());
        if (stationId.isPresent()) {
            target.setStationId(stationId.get());
        } else {
            target.setStationId(OptionalInt.empty());
            target.setProcessingResult(ProcessingResult.UNKNOWN_STATION_ICAO_CODE);
        }
    }

}
