package fi.fmi.avi.archiver.message.populator;

import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.file.InputBulletinHeading;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.ProcessingResult;
import fi.fmi.avi.model.bulletin.BulletinHeading;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;
import static fi.fmi.avi.archiver.message.populator.MessagePopulatorHelper.DEFAULT_BULLETIN_HEADING_SOURCES;
import static java.util.Objects.requireNonNull;

/**
 * When message station ICAO code matches the given pattern, validate that the bulletin originator (location indicator)
 * matches the given pattern.
 */
public class OriginatorAuthorizer implements MessagePopulator {

    private final Pattern originatorPattern;
    private final Pattern stationPattern;
    private List<BulletinHeadingSource> bulletinHeadingSources = DEFAULT_BULLETIN_HEADING_SOURCES;

    public OriginatorAuthorizer(final Pattern originatorPattern, final Pattern stationPattern) {
        this.originatorPattern = requireNonNull(originatorPattern, "originatorPattern");
        this.stationPattern = requireNonNull(stationPattern, "stationPattern");
    }

    public void setBulletinHeadingSources(final List<BulletinHeadingSource> bulletinHeadingSources) {
        requireNonNull(bulletinHeadingSources, "bulletinHeadingSources");
        checkArgument(!bulletinHeadingSources.isEmpty(), "bulletinHeadingSources cannot be empty");
        this.bulletinHeadingSources = bulletinHeadingSources;
    }

    @Override
    public void populate(final InputAviationMessage inputAviationMessage, final ArchiveAviationMessage.Builder builder) {
        requireNonNull(inputAviationMessage, "inputAviationMessage");
        requireNonNull(builder, "builder");

        final Optional<String> originator = MessagePopulatorHelper.getFirstNonNullFromBulletinHeading(bulletinHeadingSources,
                inputAviationMessage, InputBulletinHeading::getBulletinHeading).map(BulletinHeading::getLocationIndicator);
        final Optional<String> stationIcaoCode = MessagePopulatorHelper.tryGet(builder, reader -> reader.getStationIcaoCode());
        if (originator.isPresent() && stationIcaoCode.isPresent()) {
            if (stationPattern.matcher(stationIcaoCode.get()).matches() && !originatorPattern.matcher(originator.get()).matches()) {
                builder.setProcessingResult(ProcessingResult.FORBIDDEN_BULLETIN_LOCATION_INDICATOR);
            }
        }
    }

}
