package fi.fmi.avi.archiver.message.populator;

import static java.util.Objects.requireNonNull;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.file.InputBulletinHeading;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessageIWXXMDetails;
import fi.fmi.avi.archiver.util.TimeUtil;
import fi.fmi.avi.model.GenericAviationWeatherMessage;
import fi.fmi.avi.model.MessageType;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.bulletin.BulletinHeading;
import fi.fmi.avi.model.bulletin.DataTypeDesignatorT1;

/**
 * Populate {@link ArchiveAviationMessage.Builder} properties from bulletin heading data in {@link InputAviationMessage}.
 * Input source can be GTS bulletin heading, IWXXM collect identifier or both in selected order.
 *
 * <p>
 * The input source is selected in the order of provided {@link BulletinHeadingSource} list. The selection is made individually for each property, not based
 * on existence of specified source. If a value cannot be obtained from primary source (source is either missing or does not contain a specific value), then
 * following sources are attempted until a value is found or the list is out of sources.
 * </p>
 *
 * <p>
 * Message {@link ArchiveAviationMessage#getFormat()} and {@link ArchiveAviationMessage#getType()} are inferred from data type designators. Values are mapped
 * from object to id number by mappings provided as constructor parameters.
 * </p>
 *
 * <p>
 * Other properties being populated include
 * </p>
 * <ul>
 *     <li>{@link BulletinHeading#getIssueTime()} as {@link ArchiveAviationMessage#getMessageTime()}</li>
 *     <li>{@link BulletinHeading#getLocationIndicator()} as {@link ArchiveAviationMessage#getIcaoAirportCode()}</li>
 *     <li>{@link ArchiveAviationMessage#getHeading()}</li>
 *     <li>{@link ArchiveAviationMessageIWXXMDetails#getCollectIdentifier()}</li>
 *     <li>{@link ArchiveAviationMessage#getVersion()}</li>
 * </ul>
 */
public class BulletinHeadingDataPopulator implements MessagePopulator {
    private final Map<GenericAviationWeatherMessage.Format, Integer> formatIds;
    private final Map<MessageType, Integer> typeIds;
    private final List<BulletinHeadingSource> bulletinHeadingSources;

    public BulletinHeadingDataPopulator(final Map<GenericAviationWeatherMessage.Format, Integer> formatIds, final Map<MessageType, Integer> typeIds, final List<BulletinHeadingSource> bulletinHeadingSources) {
        this.formatIds = requireNonNull(formatIds, "formatIds");
        this.typeIds = requireNonNull(typeIds, "typeIds");
        this.bulletinHeadingSources = requireNonNull(bulletinHeadingSources, "bulletinHeadingSources");
    }

    @Override
    public void populate(final InputAviationMessage input, final ArchiveAviationMessage.Builder builder) {
        requireNonNull(input, "input");
        requireNonNull(builder, "builder");
        getFirstNonNullFromBulletinHeading(input, inputHeading -> inputHeading.getBulletinHeading()//
                .map(heading -> DataTypeDesignatorT1.AVIATION_INFORMATION_IN_XML.equals(heading.getDataTypeDesignatorT1ForTAC())
                        ? GenericAviationWeatherMessage.Format.IWXXM
                        : GenericAviationWeatherMessage.Format.TAC))//
                .map(formatIds::get)//
                .ifPresent(builder::setFormat);
        getFirstNonNullFromBulletinHeading(input, inputHeading -> inputHeading.getBulletinHeading()//
                .flatMap(BulletinHeading::getExpectedContainedMessageType)//
                .map(typeIds::get))//
                .ifPresent(builder::setType);
        getFirstNonNullFromBulletinHeading(input, inputHeading -> inputHeading.getBulletinHeading()//
                .flatMap(heading -> resolveCompleteInstant(heading.getIssueTime(), input)))//
                .ifPresent(builder::setMessageTime);
        getFirstNonNullFromBulletinHeading(input, inputHeading -> inputHeading.getBulletinHeading()//
                .map(BulletinHeading::getLocationIndicator))//
                .ifPresent(builder::setIcaoAirportCode);
        input.getGtsBulletinHeading()//
                .getBulletinHeadingString()//
                .ifPresent(builder::setHeading);
        getFirstNonNullFromBulletinHeading(input, inputHeading -> inputHeading.getBulletinHeading()//
                .flatMap(heading -> heading.getType() == BulletinHeading.Type.NORMAL //
                        ? Optional.empty() //
                        : heading.getBulletinAugmentationNumber()//
                                .map(augmentationNumber -> heading.getType().getPrefix() + String.valueOf(Character.toChars('A' + augmentationNumber - 1)))))//
                .ifPresent(builder::setVersion);
        input.getCollectIdentifier()//
                .getBulletinHeadingString()//
                .ifPresent(collectIdentifier -> builder.getIWXXMDetailsBuilder().setCollectIdentifier(collectIdentifier));
    }

    private Optional<Instant> resolveCompleteInstant(final PartialOrCompleteTimeInstant messageTime, final InputAviationMessage input) {
        return resolveCompleteTime(messageTime, input).map(ZonedDateTime::toInstant);
    }

    private Optional<ZonedDateTime> resolveCompleteTime(final PartialOrCompleteTimeInstant messageTime, final InputAviationMessage input) {
        final PartialOrCompleteTimeInstant zonedMessageTime = messageTime.toBuilder()//
                .mapPartialTime(partialDateTime -> partialDateTime.withZone(partialDateTime.getZone().orElse(ZoneOffset.UTC)))//
                .build();
        return TimeUtil.toCompleteTime(zonedMessageTime, //
                input.getFileMetadata().getFilenamePattern().getTimestamp().orElse(null), //
                PartialOrCompleteTimeInstant.of(input.getFileMetadata().getFileModified().atZone(ZoneOffset.UTC)));
    }

    private <T> Optional<T> getFirstNonNullFromBulletinHeading(final InputAviationMessage input, final Function<InputBulletinHeading, Optional<T>> fn) {
        return bulletinHeadingSources.stream()//
                .map(source -> fn.apply(source.get(input)).orElse(null))//
                .filter(Objects::nonNull)//
                .findFirst();
    }

    public enum BulletinHeadingSource {
        GTS_BULLETIN_HEADING {
            @Override
            InputBulletinHeading get(final InputAviationMessage inputAviationMessage) {
                return inputAviationMessage.getGtsBulletinHeading();
            }
        }, COLLECT_IDENTIFIER {
            @Override
            InputBulletinHeading get(final InputAviationMessage inputAviationMessage) {
                return inputAviationMessage.getCollectIdentifier();
            }
        };

        abstract InputBulletinHeading get(final InputAviationMessage inputAviationMessage);
    }
}
