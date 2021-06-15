package fi.fmi.avi.archiver.message;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import fi.fmi.avi.archiver.file.FileAviationMessage;
import fi.fmi.avi.archiver.file.FilenamePattern;
import fi.fmi.avi.model.*;
import fi.fmi.avi.model.bulletin.BulletinHeading;
import org.springframework.integration.annotation.ServiceActivator;

import javax.annotation.Nullable;
import java.time.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Collectors;

import static fi.fmi.avi.model.MessageType.*;
import static java.util.Objects.requireNonNull;

public class MessageParser {

    private static final MessageType LOW_WIND = new MessageType("LOW_WIND");
    private static final MessageType WX_WARNING = new MessageType("WX_WARNING");
    private final Clock clock;

    private final Map<MessageType, Integer> types;

    public MessageParser(final Clock clock, final Map<MessageType, Integer> types) {
        this.clock = requireNonNull(clock, "clock");
        this.types = requireNonNull(types, "types");
    }

    /**
     * Certain message types get their airport code from the bulletin heading and others from the message itself.
     *
     * @param bulletinHeading bulletin heading
     * @param aerodrome       aerodrome
     * @param messageType     message type
     * @return airport icao code
     */
    private static String getAirportCode(final BulletinHeading bulletinHeading, @Nullable final Aerodrome aerodrome,
                                         final MessageType messageType) {
        if (messageType.equals(WX_WARNING)) {
            return aerodrome != null ? aerodrome.getDesignator() : bulletinHeading.getLocationIndicator();
        } else if (ImmutableSet.of(TAF, METAR, SPECI, LOW_WIND).contains(messageType)) {
            if (aerodrome == null) {
                throw new IllegalStateException("No target aerodrome");
            }
            return aerodrome.getDesignator();
        } else {
            return bulletinHeading.getLocationIndicator();
        }
    }

    @ServiceActivator
    public List<AviationMessage> parse(final List<FileAviationMessage> fileAviationMessages) {
        final Instant currentTime = clock.instant();
        return fileAviationMessages.stream().map(fileMessage -> {
            // TODO Assume that the GTS heading is present for now
            final BulletinHeading bulletinHeading = fileMessage.getGtsBulletinHeading().getBulletinHeading().get();

            if (!fileMessage.getMessage().getMessageType().isPresent()) {
                throw new IllegalStateException("Unable to parse message type");
            }
            final MessageType messageType = fileMessage.getMessage().getMessageType().get();
            if (!types.containsKey(messageType)) {
                throw new IllegalStateException("Unknown message type");
            }
            final Integer typeId = types.get(messageType);

            Optional<String> version = Optional.empty();
            if (bulletinHeading.getType() != BulletinHeading.Type.NORMAL) {
                final int augmentationNumber = bulletinHeading
                        .getBulletinAugmentationNumber()
                        .orElseThrow(() -> new IllegalStateException("Heading type is not normal but augmentation number is missing"));
                version = Optional.of(
                        bulletinHeading.getType().getPrefix() + String.valueOf(Character.toChars('A' + augmentationNumber - 1)));
            }

            final String airportCode = getAirportCode(bulletinHeading, fileMessage.getMessage().getTargetAerodrome().orElse(null), messageType);

            // Get partial issue time from message or bulletin heading and try to complete it
            final PartialOrCompleteTimeInstant issueTime = fileMessage.getMessage().getIssueTime().isPresent()
                    ? fileMessage.getMessage().getIssueTime().get()
                    : bulletinHeading.getIssueTime();
            final Instant issueInstant = getIssueTime(issueTime, fileMessage.getFileMetadata().getFilenamePattern(),
                    currentTime, fileMessage.getFileMetadata().getFileModified());

            Optional<Instant> validTimeStart = Optional.empty();
            Optional<Instant> validTimeEnd = Optional.empty();
            if (fileMessage.getMessage().getValidityTime().isPresent()) {
                final Optional<ValidityTime> validityTime = getValidityTime(fileMessage.getMessage().getValidityTime().get(),
                        fileMessage.getFileMetadata().getFilenamePattern(), currentTime, fileMessage.getFileMetadata().getFileModified());
                if (validityTime.isPresent()) {
                    validTimeStart = Optional.of(validityTime.get().getStart());
                    validTimeEnd = Optional.of(validityTime.get().getEnd());
                }
            }

            return AviationMessage.builder()//
                    .setHeading(fileMessage.getGtsBulletinHeading().getBulletinHeadingString().get())// TODO
                    .setIcaoAirportCode(airportCode)//
                    .setMessage(fileMessage.getMessage().getOriginalMessage())//
                    .setMessageTime(issueInstant)//
                    .setRoute(1)// TODO
                    .setType(typeId)//
                    .setValidFrom(validTimeStart)//
                    .setValidTo(validTimeEnd)//
                    .setFileModified(fileMessage.getFileMetadata().getFileModified())//
                    .setVersion(version)//
                    .build();
        }).collect(Collectors.toList());
    }

    /**
     * Get a complete issue time from the given (partial) issue time. Completion is attempted using one of the following as reference:
     * 1) File timestamp
     * 2) File modification time
     * <p>
     * If neither is available, current time is returned.
     *
     * @param issueTime        issue time that will be completed if it is partial
     * @param filenamePattern  message file pattern for the given message type
     * @param currentTime      current time
     * @param fileLastModified last modified time of the file
     * @return complete issue time
     */
    private Instant getIssueTime(final PartialOrCompleteTimeInstant issueTime, final FilenamePattern filenamePattern,
                                 final Instant currentTime, @Nullable final Instant fileLastModified) {
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

            OptionalInt year = getTemporalComponent(filenamePattern, FilenamePattern.YEAR);
            OptionalInt month = getTemporalComponent(filenamePattern, FilenamePattern.MONTH);

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
     * @param validityPeriod  validity period that will be completed if it is partial
     * @param filenamePattern message file pattern for the given message type
     * @param currentTime     current time
     * @param fileModified    last modified time of the file
     * @return completed validity time or an empty optional if completion is not possible
     */
    private Optional<ValidityTime> getValidityTime(final PartialOrCompleteTimePeriod validityPeriod, final FilenamePattern filenamePattern,
                                                   final Instant currentTime, @Nullable final Instant fileModified) {
        if (validityPeriod.isCompleteStrict()) {
            return Optional.of(ValidityTime.create(validityPeriod));
        } else {
            OptionalInt year = getTemporalComponent(filenamePattern, FilenamePattern.YEAR);
            OptionalInt month = getTemporalComponent(filenamePattern, FilenamePattern.MONTH);
            OptionalInt day = getTemporalComponent(filenamePattern, FilenamePattern.DAY);
            OptionalInt hour = getTemporalComponent(filenamePattern, FilenamePattern.HOUR);
            OptionalInt minute = getTemporalComponent(filenamePattern, FilenamePattern.MINUTE);

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
            return new AutoValue_MessageParser_ValidityTime(start, end);
        }

        abstract Instant getStart();

        abstract Instant getEnd();
    }

}
