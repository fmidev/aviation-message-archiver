package fi.fmi.avi.archiver.message.validator;

import fi.fmi.avi.archiver.message.AviationMessage;
import fi.fmi.avi.archiver.message.ProcessingResult;
import org.springframework.stereotype.Component;

@Component
public class AirportICAOCodeValidator implements MessageValidator {

    @Override
    public AviationMessage.Builder validate(AviationMessage.Builder message) {
        if (!message.getStationId().isPresent()) {
            message.setProcessingResult(ProcessingResult.UNKNOWN_ICAO_CODE);
        }
        return message;
    }

}
