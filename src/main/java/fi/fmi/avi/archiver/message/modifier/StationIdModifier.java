package fi.fmi.avi.archiver.message.modifier;

import fi.fmi.avi.archiver.database.DatabaseAccess;
import fi.fmi.avi.archiver.message.AviationMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class StationIdModifier implements MessageModifier {

    @Autowired
    private DatabaseAccess databaseAccess;

    @Override
    public AviationMessage.Builder modify(final AviationMessage.Builder message) {
        final Optional<Integer> stationId = databaseAccess.queryStationId(message.getIcaoAirportCode());
        stationId.ifPresent(message::setStationId);
        return message;
    }

}
