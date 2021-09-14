package fi.fmi.avi.archiver.message.populator;

import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.ProcessingResult;
import fi.fmi.avi.model.GenericAviationWeatherMessage;

import javax.annotation.Nullable;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;

/**
 * When the message aerodrome matches the given pattern, validate that the bulletin location indicator matches the given pattern.
 */
public class BulletinLocationIndicatorValidator implements MessagePopulator {

    private Pattern bulletinLocationIndicatorPattern;
    private Pattern messageAerodromePattern;

    public void setBulletinLocationIndicatorPattern(final Pattern bulletinLocationIndicatorPattern) {
        this.bulletinLocationIndicatorPattern = requireNonNull(bulletinLocationIndicatorPattern, "bulletinLocationIndicatorPattern");
    }

    public void setMessageAerodromePattern(final Pattern messageAerodromePattern) {
        this.messageAerodromePattern = requireNonNull(messageAerodromePattern, "messageAerodromePattern");
    }

    @Override
    public void populate(@Nullable final InputAviationMessage inputAviationMessage, final ArchiveAviationMessage.Builder builder) {
        requireNonNull(inputAviationMessage, "inputAviationMessage");
        requireNonNull(builder, "builder");
        if (inputAviationMessage.getGtsBulletinHeading().getBulletinHeading().isPresent()
                && inputAviationMessage.getMessage().getLocationIndicators().containsKey(GenericAviationWeatherMessage.LocationIndicatorType.AERODROME)) {
            final String locationIndicator = inputAviationMessage.getGtsBulletinHeading().getBulletinHeading().get().getLocationIndicator();
            final String aerodrome = inputAviationMessage.getMessage().getLocationIndicators().get(GenericAviationWeatherMessage.LocationIndicatorType.AERODROME);
            if (messageAerodromePattern.matcher(aerodrome).matches()
                    && !bulletinLocationIndicatorPattern.matcher(locationIndicator).matches()) {
                builder.setProcessingResult(ProcessingResult.INVALID_BULLETIN_LOCATION_INDICATOR);
            }
        }
    }

}
