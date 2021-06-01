package fi.fmi.avi.archiver.message;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import fi.fmi.avi.archiver.initializing.MessageFileMonitorInitializer;
import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.tac.conf.TACConverter;
import fi.fmi.avi.model.*;
import fi.fmi.avi.model.bulletin.BulletinHeading;
import fi.fmi.avi.model.bulletin.GenericMeteorologicalBulletin;
import fi.fmi.avi.util.BulletinHeadingEncoder;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.MessageHeaders;

import javax.annotation.Nullable;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

import static fi.fmi.avi.model.MessageType.*;
import static java.util.Objects.requireNonNull;

public class MessageParser {

    private static final MessageType LOW_WIND = new MessageType("LOW_WIND");
    private static final MessageType WX_WARNING = new MessageType("WX_WARNING");
    private final Clock clock;

    private final AviMessageConverter aviMessageConverter;
    private final Map<MessageType, Integer> types;

    public MessageParser(final Clock clock, final AviMessageConverter aviMessageConverter, final Map<MessageType, Integer> types) {
        this.clock = requireNonNull(clock, "clock");
        this.aviMessageConverter = requireNonNull(aviMessageConverter, "aviMessageConverter");
        this.types = requireNonNull(types, "types");
    }

    /**
     * Certain message types get their airport code from the bulletin heading and others from the message itself.
     *
     * @param bulletinHeading        bulletin heading
     * @param aviationWeatherMessage aviation weather message
     * @param messageType            message type
     * @return airport icao code
     */
    private static String getAirportCode(final BulletinHeading bulletinHeading, final GenericAviationWeatherMessage aviationWeatherMessage,
                                         final MessageType messageType) {
        if (messageType.equals(WX_WARNING)) {
            return aviationWeatherMessage.getTargetAerodrome().isPresent()
                    ? aviationWeatherMessage.getTargetAerodrome().get().getDesignator()
                    : bulletinHeading.getLocationIndicator();
        } else if (ImmutableSet.of(TAF, METAR, SPECI, LOW_WIND).contains(messageType)) {
            return aviationWeatherMessage.getTargetAerodrome().orElseThrow(() -> new IllegalStateException("No target aerodrome")).getDesignator();
        } else {
            return bulletinHeading.getLocationIndicator();
        }
    }

    @ServiceActivator
    public List<AviationMessage> parse(final String content, final MessageHeaders headers) {
        // TODO Bring current time from outside? Possibly in headers?
        final Instant now = clock.instant();
        final AviationMessageFilenamePattern pattern = (AviationMessageFilenamePattern) headers.get(MessageFileMonitorInitializer.MESSAGE_FILE_PATTERN);
        final Instant fileLastModified = (Instant) headers.get(MessageFileMonitorInitializer.FILE_LAST_MODIFIED);
        return parse(0, pattern, content.trim(), now, fileLastModified);
    }

    public List<AviationMessage> parse(final int routeId, final AviationMessageFilenamePattern messageFilePattern, final String content,
                                       final Instant currentTime, @Nullable final Instant fileLastModified) {
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

                        final String airportCode = getAirportCode(bulletin.getHeading(), message, message.getMessageType().get());

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

                        return AviationMessage.builder()//
                                .setHeading(BulletinHeadingEncoder.encode(bulletin.getHeading(), null))//
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
                    }).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    /**
     * Get a complete issue time from the given (partial) issue time. Completion is attempted using one of the following as reference:
     * 1) File timestamp
     * 2) File modification time
     * <p>
     * If neither is available, current time is returned.
     *
     * @param issueTime          issue time that will be completed if it is partial
     * @param messageFilePattern message file pattern for the given message type
     * @param currentTime        current time
     * @param fileLastModified   last modified time of the file
     * @return complete issue time
     */
    private Instant getIssueTime(final PartialOrCompleteTimeInstant issueTime, final AviationMessageFilenamePattern messageFilePattern,
                                 final Instant currentTime, @Nullable final Instant fileLastModified) {
        if (issueTime.getCompleteTime().isPresent()) {
            return issueTime.getCompleteTime().get().toInstant();
        }

        // Use partial issue time and values parsed from filename
        else if (issueTime.getPartialTime().isPresent()) {
            OptionalInt minute = issueTime.getMinute();
            if (!minute.isPresent()) {
                minute = getTemporalComponent(messageFilePattern, AviationMessageFilenamePattern.MINUTE);
            }

            OptionalInt hour = issueTime.getHour();
            if (!hour.isPresent()) {
                hour = getTemporalComponent(messageFilePattern, AviationMessageFilenamePattern.HOUR);
            }

            OptionalInt day = issueTime.getDay();
            if (!day.isPresent()) {
                day = getTemporalComponent(messageFilePattern, AviationMessageFilenamePattern.DAY);
            }

            OptionalInt year = getTemporalComponent(messageFilePattern, AviationMessageFilenamePattern.YEAR);
            OptionalInt month = getTemporalComponent(messageFilePattern, AviationMessageFilenamePattern.MONTH);

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
     * @param validityPeriod     validity period that will be completed if it is partial
     * @param messageFilePattern message file pattern for the given message type
     * @param currentTime        current time
     * @param fileLastModified   last modified time of the file
     * @return completed validity time or an empty optional if completion is not possible
     */
    private Optional<ValidityTime> getValidityTime(final PartialOrCompleteTimePeriod validityPeriod, final AviationMessageFilenamePattern messageFilePattern,
                                                   final Instant currentTime, @Nullable final Instant fileLastModified) {
        if (validityPeriod.isCompleteStrict()) {
            return Optional.of(ValidityTime.create(validityPeriod));
        } else {
            OptionalInt year = getTemporalComponent(messageFilePattern, AviationMessageFilenamePattern.YEAR);
            OptionalInt month = getTemporalComponent(messageFilePattern, AviationMessageFilenamePattern.MONTH);
            OptionalInt day = getTemporalComponent(messageFilePattern, AviationMessageFilenamePattern.DAY);
            OptionalInt hour = getTemporalComponent(messageFilePattern, AviationMessageFilenamePattern.HOUR);
            OptionalInt minute = getTemporalComponent(messageFilePattern, AviationMessageFilenamePattern.MINUTE);

            if (year.isPresent() && month.isPresent() && day.isPresent() && hour.isPresent() && minute.isPresent()) {
                final ZonedDateTime reference = ZonedDateTime.of(
                        LocalDateTime.of(year.getAsInt(), month.getAsInt(), day.getAsInt(), hour.getAsInt(), minute.getAsInt()), clock.getZone());
                final PartialOrCompleteTimePeriod completeValidityPeriod = validityPeriod.toBuilder().completePartialStartingNear(reference).build();
                if (completeValidityPeriod.isCompleteStrict()) {
                    return Optional.of(ValidityTime.create(completeValidityPeriod));
                }
            }

            // Use file last modified
            if (fileLastModified != null) {
                final PartialOrCompleteTimePeriod completeValidityPeriod = validityPeriod.toBuilder()
                        .completePartialStartingNear(fileLastModified.atZone(clock.getZone()))
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

    private OptionalInt getTemporalComponent(final AviationMessageFilenamePattern messageFilePattern, final String component) {
        try {
            return OptionalInt.of(messageFilePattern.getInt(component));
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
