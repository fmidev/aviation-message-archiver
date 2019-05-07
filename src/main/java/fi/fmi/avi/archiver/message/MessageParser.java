package fi.fmi.avi.archiver.message;

import static java.util.Objects.requireNonNull;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.google.auto.value.AutoValue;

import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.tac.conf.TACConverter;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.BulletinHeading;
import fi.fmi.avi.model.GenericMeteorologicalBulletin;
import fi.fmi.avi.model.PartialDateTime;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.PartialOrCompleteTimePeriod;

public class MessageParser {

    private static final String DELIMITER = " ";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("ddHHmm");

    private final ZoneId zone;

    private final AviMessageConverter aviMessageConverter;
    private final Map<AviationCodeListUser.MessageType, Integer> types;

    public MessageParser(final ZoneId zone, final AviMessageConverter aviMessageConverter, final Map<AviationCodeListUser.MessageType, Integer> types) {
        this.zone = zone;
        this.aviMessageConverter = requireNonNull(aviMessageConverter, "aviMessageConverter");
        this.types = requireNonNull(types, "types");
    }

    /**
     * TODO This conversion should probably be provided by the converter library.
     *
     * @param bulletinHeading
     *         bulletin heading
     *
     * @return bulletin heading serialized into string
     */
    private static String serialize(final BulletinHeading bulletinHeading) {
        requireNonNull(bulletinHeading, "bulletinHeading");
        final PartialOrCompleteTimeInstant issueTime = bulletinHeading.getIssueTime();

        final String time = issueTime.getCompleteTime()
                .map(zonedDateTime -> zonedDateTime.format(DATE_TIME_FORMATTER))
                .orElse(issueTime.getPartialTime()
                        .map(partialTime -> String.format("%02d%02d%02d", partialTime.getDay().orElse(0), partialTime.getHour().orElse(0),
                                partialTime.getMinute().orElse(0)))
                        .orElse("000000"));

        final StringBuilder sb = new StringBuilder();
        sb.append(bulletinHeading.getDataTypeDesignatorsForTAC())//
                .append(bulletinHeading.getGeographicalDesignator())//
                .append(bulletinHeading.getBulletinNumber())//
                .append(DELIMITER)//
                .append(bulletinHeading.getLocationIndicator())//
                .append(DELIMITER)//
                .append(time);

        if (bulletinHeading.getType() != BulletinHeading.Type.NORMAL && bulletinHeading.getBulletinAugmentationNumber().isPresent()) {
            final String augmentationNumber = String.valueOf(Character.toChars('A' + bulletinHeading.getBulletinAugmentationNumber().get() - 1));
            sb.append(DELIMITER)//
                    .append(bulletinHeading.getType().getPrefix())//
                    .append(augmentationNumber);
        }
        return sb.toString();
    }

    public Collection<Message> parse(final int routeId, final MessageFilenamePattern messageFilePattern, final String content, final Instant currentTime,
            @Nullable final Instant fileLastModified) {
        final ConversionResult<GenericMeteorologicalBulletin> bulletinConversion = aviMessageConverter.convertMessage(content,
                TACConverter.TAC_TO_GENERIC_BULLETIN_POJO);
        if (bulletinConversion.getConvertedMessage().isPresent()) {
            final GenericMeteorologicalBulletin bulletin = bulletinConversion.getConvertedMessage().get();
            return bulletin.getMessages().stream()//
                    .map(message -> { //
                        final int typeId = message.getMessageType().isPresent() ? types.get(message.getMessageType().get()) : -1;
                        if (typeId == -1) {
                            throw new IllegalStateException("Message type cannot be determined");
                        }

                        Optional<String> version = Optional.empty();
                        if (bulletin.getHeading().getType() != BulletinHeading.Type.NORMAL) {
                            final int augmentationNumber = bulletin.getHeading()
                                    .getBulletinAugmentationNumber()
                                    .orElseThrow(() -> new IllegalStateException("Heading type is not normal but augmentation number is missing"));
                            version = Optional.of(
                                    bulletin.getHeading().getType().getPrefix() + String.valueOf(Character.toChars('A' + augmentationNumber - 1)));
                        }

                        // Message aerodrome or bulletin location indicator as fallback
                        final String airportCode = message.getTargetAerodrome().isPresent()
                                ? message.getTargetAerodrome().get().getDesignator()
                                : bulletin.getHeading().getLocationIndicator();

                        // Get partial issue time from message or bulletin heading and try to complete it
                        final PartialOrCompleteTimeInstant issueTime = message.getIssueTime().isPresent()
                                ? message.getIssueTime().get()
                                : bulletin.getHeading().getIssueTime();
                        final Instant issueInstant = getIssueTime(issueTime, messageFilePattern, currentTime, fileLastModified);

                        Optional<Instant> validTimeStart = Optional.empty();
                        Optional<Instant> validTimeEnd = Optional.empty();
                        if (message.getValidityTime().isPresent()) {
                            final Optional<ValidityTime> validityTime = getValidityTime(message.getValidityTime().get(), messageFilePattern, currentTime,
                                    fileLastModified);
                            if (validityTime.isPresent()) {
                                validTimeStart = Optional.of(validityTime.get().getStart());
                                validTimeEnd = Optional.of(validityTime.get().getEnd());
                            }
                        }

                        return Message.builder()//
                                .setHeading(serialize(bulletin.getHeading()))//
                                .setIcaoAirportCode(airportCode)//
                                .setMessage(message.getOriginalMessage())//
                                .setMessageTime(issueInstant)//
                                .setRoute(routeId)//
                                .setType(typeId)//
                                .setValidFrom(validTimeStart)//
                                .setValidTo(validTimeEnd)//
                                .setNullableFileModified(fileLastModified)//
                                .setVersion(version)//
                                .build();
                    }).collect(Collectors.toSet());
        }
        return Collections.emptySet();
    }

    private Instant getIssueTime(final PartialOrCompleteTimeInstant issueTime, final MessageFilenamePattern messageFilePattern, final Instant currentTime,
            @Nullable final Instant fileLastModified) {
        if (issueTime.getCompleteTime().isPresent()) {
            return issueTime.getCompleteTime().get().toInstant();
        }

        // Use partial issue time and values parsed from filename
        else if (issueTime.getPartialTime().isPresent()) {
            OptionalInt minute = issueTime.getMinute();
            if (!minute.isPresent()) {
                minute = getTemporalComponent(messageFilePattern, MessageFilenamePattern.MINUTE);
            }

            OptionalInt hour = issueTime.getHour();
            if (!hour.isPresent()) {
                hour = getTemporalComponent(messageFilePattern, MessageFilenamePattern.HOUR);
            }

            OptionalInt day = issueTime.getDay();
            if (!day.isPresent()) {
                day = getTemporalComponent(messageFilePattern, MessageFilenamePattern.DAY);
            }

            OptionalInt year = getTemporalComponent(messageFilePattern, MessageFilenamePattern.YEAR);
            OptionalInt month = getTemporalComponent(messageFilePattern, MessageFilenamePattern.MONTH);

            if (year.isPresent() && month.isPresent() && day.isPresent() && hour.isPresent() && minute.isPresent()) {
                return PartialDateTime.of(day.getAsInt(), hour.getAsInt(), minute.getAsInt(), zone)
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

    private OptionalInt getTemporalComponent(final MessageFilenamePattern messageFilePattern, final String component) {
        try {
            return OptionalInt.of(messageFilePattern.getInt(component));
        } catch (final Throwable t) {
            // There's no group with the given name or it is not parseable into an integer
            return OptionalInt.empty();
        }
    }

    private Optional<ValidityTime> getValidityTime(final PartialOrCompleteTimePeriod validityPeriod, final MessageFilenamePattern messageFilePattern,
            final Instant currentTime, @Nullable final Instant fileLastModified) {
        if (validityPeriod.isCompleteStrict()) {
            return Optional.of(ValidityTime.create(validityPeriod));
        } else {
            // Use file timestamp as completion time. then file modified and last current time
            OptionalInt year = getTemporalComponent(messageFilePattern, MessageFilenamePattern.YEAR);
            OptionalInt month = getTemporalComponent(messageFilePattern, MessageFilenamePattern.MONTH);
            OptionalInt day = getTemporalComponent(messageFilePattern, MessageFilenamePattern.DAY);
            OptionalInt hour = getTemporalComponent(messageFilePattern, MessageFilenamePattern.HOUR);
            OptionalInt minute = getTemporalComponent(messageFilePattern, MessageFilenamePattern.MINUTE);

            if (year.isPresent() && month.isPresent() && day.isPresent() && hour.isPresent() && minute.isPresent()) {
                final ZonedDateTime reference = ZonedDateTime.of(
                        LocalDateTime.of(year.getAsInt(), month.getAsInt(), day.getAsInt(), hour.getAsInt(), minute.getAsInt()), zone);
                final PartialOrCompleteTimePeriod completeValidityPeriod = validityPeriod.toBuilder().completePartialStartingNear(reference).build();
                if (completeValidityPeriod.isCompleteStrict()) {
                    return Optional.of(ValidityTime.create(completeValidityPeriod));
                }
            }

            // Use file last modified
            if (fileLastModified != null) {
                final PartialOrCompleteTimePeriod completeValidityPeriod = validityPeriod.toBuilder()
                        .completePartialStartingNear(fileLastModified.atZone(zone))
                        .build();
                if (completeValidityPeriod.isCompleteStrict()) {
                    return Optional.of(ValidityTime.create(completeValidityPeriod));
                }
            }

            // Use current time
            final PartialOrCompleteTimePeriod completeValidityPeriod = validityPeriod.toBuilder().completePartialStartingNear(currentTime.atZone(zone)).build();
            if (completeValidityPeriod.isCompleteStrict()) {
                return Optional.of(ValidityTime.create(completeValidityPeriod));
            }
        }

        return Optional.empty();
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
