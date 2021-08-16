package fi.fmi.avi.archiver.message.populator;

import static fi.fmi.avi.model.MessageType.METAR;
import static fi.fmi.avi.model.MessageType.SPECI;
import static fi.fmi.avi.model.MessageType.TAF;
import static java.util.Objects.requireNonNull;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import javax.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;

import fi.fmi.avi.archiver.file.FilenamePattern;
import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.model.GenericAviationWeatherMessage;
import fi.fmi.avi.model.MessageType;
import fi.fmi.avi.model.PartialDateTime;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.PartialOrCompleteTimePeriod;
import fi.fmi.avi.model.bulletin.BulletinHeading;

public class BaseDataPopulator implements MessagePopulator {

    private static final MessageType LOW_WIND = new MessageType("LOW_WIND");
    private static final MessageType WX_WARNING = new MessageType("WX_WARNING");

    private final Clock clock;
    private final Map<MessageType, Integer> types;

    public BaseDataPopulator(final Clock clock, final Map<MessageType, Integer> types) {
        this.clock = requireNonNull(clock, "clock");
        this.types = requireNonNull(types, "types");
    }

    /**
     * Certain message types get their airport code from the bulletin heading and others from the message itself.
     *
     * @param bulletinHeading
     *         bulletin heading
     * @param locationIndicator
     *         aerodrome location indicator
     * @param messageType
     *         message type
     *
     * @return airport icao code
     */
    private static String getAirportCode(final BulletinHeading bulletinHeading, @Nullable final String locationIndicator, final MessageType messageType) {
        if (messageType.equals(WX_WARNING)) {
            return locationIndicator != null ? locationIndicator : bulletinHeading.getLocationIndicator();
        } else if (ImmutableSet.of(TAF, METAR, SPECI, LOW_WIND).contains(messageType)) {
            if (locationIndicator == null) {
                throw new IllegalStateException("No target aerodrome");
            }
            return locationIndicator;
        } else {
            return bulletinHeading.getLocationIndicator();
        }
    }

    @Override
    public void populate(final InputAviationMessage inputAviationMessage, final ArchiveAviationMessage.Builder aviationMessageBuilder) {
        final Instant currentTime = clock.instant();
        // TODO Assume that the GTS heading is present for now
        final BulletinHeading bulletinHeading = inputAviationMessage.getGtsBulletinHeading().getBulletinHeading().get();

        if (!inputAviationMessage.getMessage().getMessageType().isPresent()) {
            throw new IllegalStateException("Unable to parse message type");
        }
        final MessageType messageType = inputAviationMessage.getMessage().getMessageType().get();
        if (!types.containsKey(messageType)) {
            throw new IllegalStateException("Unknown message type");
        }
        final Integer typeId = types.get(messageType);

        Optional<String> version = Optional.empty();
        if (bulletinHeading.getType() != BulletinHeading.Type.NORMAL) {
            final int augmentationNumber = bulletinHeading.getBulletinAugmentationNumber()
                    .orElseThrow(() -> new IllegalStateException("Heading type is not normal but augmentation number is missing"));
            version = Optional.of(bulletinHeading.getType().getPrefix() + String.valueOf(Character.toChars('A' + augmentationNumber - 1)));
        }

        final String messageAerodromeIndicator = inputAviationMessage.getMessage()
                .getLocationIndicators()
                .getOrDefault(GenericAviationWeatherMessage.LocationIndicatorType.AERODROME, null);
        final String airportCode = getAirportCode(bulletinHeading, messageAerodromeIndicator, messageType);

        // Get partial issue time from message or bulletin heading and try to complete it
        final PartialOrCompleteTimeInstant issueTime = inputAviationMessage.getMessage().getIssueTime().isPresent() //
                ? inputAviationMessage.getMessage().getIssueTime().get() //
                : bulletinHeading.getIssueTime();
        final Instant issueInstant = getIssueTime(issueTime, inputAviationMessage.getFileMetadata().getFilenamePattern(), currentTime,
                inputAviationMessage.getFileMetadata().getFileModified());

        Optional<Instant> validTimeStart = Optional.empty();
        Optional<Instant> validTimeEnd = Optional.empty();
        if (inputAviationMessage.getMessage().getValidityTime().isPresent()) {
            final Optional<ValidityTime> validityTime = getValidityTime(inputAviationMessage.getMessage().getValidityTime().get(),
                    inputAviationMessage.getFileMetadata().getFilenamePattern(), currentTime, inputAviationMessage.getFileMetadata().getFileModified());
            if (validityTime.isPresent()) {
                validTimeStart = Optional.of(validityTime.get().getStart());
                validTimeEnd = Optional.of(validityTime.get().getEnd());
            }
        }

        aviationMessageBuilder//
                .setHeading(inputAviationMessage.getGtsBulletinHeading().getBulletinHeadingString())// TODO
                .setIcaoAirportCode(airportCode)//
                .setMessage(inputAviationMessage.getMessage().getOriginalMessage())//
                .setMessageTime(issueInstant)//
                .setFormat(1)// TODO
                .setRoute(1)// TODO
                .setType(typeId)//
                .setValidFrom(validTimeStart)//
                .setValidTo(validTimeEnd)//
                .setFileModified(inputAviationMessage.getFileMetadata().getFileModified())//
                .setVersion(version)//
                .build();
    }

    /**
     * Get a complete issue time from the given (partial) issue time. Completion is attempted using one of the following as reference:
     * 1) File timestamp
     * 2) File modification time
     * <p>
     * If neither is available, current time is returned.
     *
     * @param issueTime
     *         issue time that will be completed if it is partial
     * @param filenamePattern
     *         message file pattern for the given message type
     * @param currentTime
     *         current time
     * @param fileLastModified
     *         last modified time of the file
     *
     * @return complete issue time
     */
    private Instant getIssueTime(final PartialOrCompleteTimeInstant issueTime, final FilenamePattern filenamePattern, final Instant currentTime,
            @Nullable final Instant fileLastModified) {
        if (issueTime.getCompleteTime().isPresent()) {
            return issueTime.getCompleteTime().get().toInstant();
        }

        // Use partial issue time and values parsed from filename
        else if (issueTime.getPartialTime().isPresent()) {
            OptionalInt minute = issueTime.getMinute();
            if (!minute.isPresent()) {
                minute = getTemporalComponent(filenamePattern, FilenamePattern.MINUTE);
            }

            OptionalInt hour = issueTime.getHour();
            if (!hour.isPresent()) {
                hour = getTemporalComponent(filenamePattern, FilenamePattern.HOUR);
            }

            OptionalInt day = issueTime.getDay();
            if (!day.isPresent()) {
                day = getTemporalComponent(filenamePattern, FilenamePattern.DAY);
            }

            final OptionalInt year = getTemporalComponent(filenamePattern, FilenamePattern.YEAR);
            final OptionalInt month = getTemporalComponent(filenamePattern, FilenamePattern.MONTH);

            if (year.isPresent() && month.isPresent() && day.isPresent() && hour.isPresent() && minute.isPresent()) {
                return PartialDateTime.of(day.getAsInt(), hour.getAsInt(), minute.getAsInt(), clock.getZone())
                        .toZonedDateTime(YearMonth.of(year.getAsInt(), month.getAsInt()))
                        .toInstant();
            }
        }

        // If that fails, use the file's last modified time
        if (fileLastModified != null) {
            return fileLastModified;
        }

        // Use current time as last resort
        return currentTime;
    }

    /**
     * Get a complete validity time from the given validity period. Completion is attempted using one of the following as reference:
     * 1) File timestamp
     * 2) File modification time
     * 3) Current time
     *
     * @param validityPeriod
     *         validity period that will be completed if it is partial
     * @param filenamePattern
     *         message file pattern for the given message type
     * @param currentTime
     *         current time
     * @param fileModified
     *         last modified time of the file
     *
     * @return completed validity time or an empty optional if completion is not possible
     */
    private Optional<ValidityTime> getValidityTime(final PartialOrCompleteTimePeriod validityPeriod, final FilenamePattern filenamePattern,
            final Instant currentTime, @Nullable final Instant fileModified) {
        if (validityPeriod.isCompleteStrict()) {
            return Optional.of(ValidityTime.create(validityPeriod));
        } else {
            final OptionalInt year = getTemporalComponent(filenamePattern, FilenamePattern.YEAR);
            final OptionalInt month = getTemporalComponent(filenamePattern, FilenamePattern.MONTH);
            final OptionalInt day = getTemporalComponent(filenamePattern, FilenamePattern.DAY);
            final OptionalInt hour = getTemporalComponent(filenamePattern, FilenamePattern.HOUR);
            final OptionalInt minute = getTemporalComponent(filenamePattern, FilenamePattern.MINUTE);

            if (year.isPresent() && month.isPresent() && day.isPresent() && hour.isPresent() && minute.isPresent()) {
                final ZonedDateTime reference = ZonedDateTime.of(
                        LocalDateTime.of(year.getAsInt(), month.getAsInt(), day.getAsInt(), hour.getAsInt(), minute.getAsInt()), clock.getZone());
                final PartialOrCompleteTimePeriod completeValidityPeriod = validityPeriod.toBuilder().completePartialStartingNear(reference).build();
                if (completeValidityPeriod.isCompleteStrict()) {
                    return Optional.of(ValidityTime.create(completeValidityPeriod));
                }
            }

            // Use file last modified
            if (fileModified != null) {
                final PartialOrCompleteTimePeriod completeValidityPeriod = validityPeriod.toBuilder()
                        .completePartialStartingNear(fileModified.atZone(clock.getZone()))
                        .build();
                if (completeValidityPeriod.isCompleteStrict()) {
                    return Optional.of(ValidityTime.create(completeValidityPeriod));
                }
            }

            // Use current time
            final PartialOrCompleteTimePeriod completeValidityPeriod = validityPeriod.toBuilder()
                    .completePartialStartingNear(currentTime.atZone(clock.getZone()))
                    .build();
            if (completeValidityPeriod.isCompleteStrict()) {
                return Optional.of(ValidityTime.create(completeValidityPeriod));
            }
        }

        return Optional.empty();
    }

    private OptionalInt getTemporalComponent(final FilenamePattern filenamePattern, final String component) {
        try {
            return OptionalInt.of(filenamePattern.getInt(component));
        } catch (final Throwable t) {
            // There's no group with the given name or it is not parseable into an integer
            return OptionalInt.empty();
        }
    }

    @AutoValue
    abstract static class ValidityTime {
        static ValidityTime create(final PartialOrCompleteTimePeriod completeValidityPeriod) {
            final Instant start = completeValidityPeriod.getStartTime().get().getCompleteTime().get().toInstant();
            final Instant end = completeValidityPeriod.getEndTime().get().getCompleteTime().get().toInstant();
            return new AutoValue_BaseDataPopulator_ValidityTime(start, end);
        }

        abstract Instant getStart();

        abstract Instant getEnd();
    }

}
