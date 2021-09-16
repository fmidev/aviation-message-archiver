package fi.fmi.avi.archiver.message.populator;

import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.file.InputBulletinHeading;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.ProcessingResult;
import fi.fmi.avi.model.GenericAviationWeatherMessage;
import fi.fmi.avi.model.bulletin.BulletinHeading;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;
import static fi.fmi.avi.archiver.message.populator.MessagePopulatorHelper.DEFAULT_BULLETIN_HEADING_SOURCES;
import static java.util.Objects.requireNonNull;

/**
 * When the bulletin location indicator matches the given pattern, validate that the message aerodrome matches the given pattern.
 */
public class MessageAerodromeValidator implements MessagePopulator {

    private List<BulletinHeadingSource> bulletinHeadingSources = DEFAULT_BULLETIN_HEADING_SOURCES;
    private Pattern bulletinLocationIndicatorPattern;
    private Pattern messageAerodromePattern;

    public void setBulletinHeadingSources(final List<BulletinHeadingSource> bulletinHeadingSources) {
        requireNonNull(bulletinHeadingSources, "bulletinHeadingSources");
        checkArgument(!bulletinHeadingSources.isEmpty(), "bulletinHeadingSources cannot be empty");
        this.bulletinHeadingSources = bulletinHeadingSources;
    }

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

        final Optional<String> optIndicator = MessagePopulatorHelper.getFirstNonNullFromBulletinHeading(bulletinHeadingSources,
                inputAviationMessage, InputBulletinHeading::getBulletinHeading).map(BulletinHeading::getLocationIndicator);
        if (optIndicator.isPresent()
                && inputAviationMessage.getMessage().getLocationIndicators().containsKey(GenericAviationWeatherMessage.LocationIndicatorType.AERODROME)) {
            final String locationIndicator = optIndicator.get();
            final String aerodrome = inputAviationMessage.getMessage().getLocationIndicators().get(GenericAviationWeatherMessage.LocationIndicatorType.AERODROME);
            if (bulletinLocationIndicatorPattern.matcher(locationIndicator).matches()
                    && !messageAerodromePattern.matcher(aerodrome).matches()) {
                builder.setProcessingResult(ProcessingResult.INVALID_MESSAGE_AERODROME);
            }
        }
    }

}
