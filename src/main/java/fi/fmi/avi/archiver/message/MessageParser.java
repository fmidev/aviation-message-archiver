package fi.fmi.avi.archiver.message;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.tac.conf.TACConverter;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.BulletinHeading;
import fi.fmi.avi.model.GenericMeteorologicalBulletin;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.PartialOrCompleteTimePeriod;

public class MessageParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageParser.class);

    private final Clock clock;
    private final AviMessageConverter aviMessageConverter;
    private final Map<AviationCodeListUser.MessageType, Integer> types;

    public MessageParser(final Clock clock, final AviMessageConverter aviMessageConverter, final Map<AviationCodeListUser.MessageType, Integer> types) {
        this.clock = requireNonNull(clock, "clock");
        this.aviMessageConverter = requireNonNull(aviMessageConverter, "aviMessageConverter");
        this.types = requireNonNull(types, "types");
    }

    public void parse(final Path filePath, final String content) {
        // TODO Is route id determined here or earlier in the chain?
        // TODO File modified?
        try {
            Instant fileModified = Files.getLastModifiedTime(filePath).toInstant();
            parse(0, filePath.getFileName().toString(), content, fileModified);
        } catch (final IOException e) {
            LOGGER.error("Unable to get file's last modified time: {}", filePath.toString(), e);
            parse(0, filePath.getFileName().toString(), content, null);
        }
    }

    public Collection<Message> parse(final int routeId, final String filename, final String content, @Nullable final Instant fileModified) {
        final ConversionResult<GenericMeteorologicalBulletin> bulletinConversion = aviMessageConverter.convertMessage(content,
                TACConverter.TAC_TO_GENERIC_BULLETIN_POJO);
        if (bulletinConversion.getConvertedMessage().isPresent()) {
            final GenericMeteorologicalBulletin bulletin = bulletinConversion.getConvertedMessage().get();

            return bulletin.getMessages().stream()//
                    .map(message -> { //
                        final String heading = bulletin.getHeading().toString();

                        // TODO Message aerodrome or if that is missing then bulletin aerodrome?
                        final String airportCode = message.getTargetAerodrome().isPresent()
                                ? message.getTargetAerodrome().get().getDesignator()
                                : bulletin.getHeading().getLocationIndicator();

                        // TODO This should be completed from the filename timestamp? Or from current time? Use message issue time first and heading issue time as fallback?
                        final PartialOrCompleteTimeInstant issueTime = message.getIssueTime().isPresent()
                                ? message.getIssueTime().get()
                                : bulletin.getHeading().getIssueTime();
                        final PartialOrCompleteTimeInstant completeIssueTime = issueTime.toBuilder().completePartialNear(ZonedDateTime.now(clock)).build();
                        final Instant issueInstant = completeIssueTime.getCompleteTime().isPresent()
                                ? completeIssueTime.getCompleteTime().get().toInstant()
                                : clock.instant();

                        // TODO What to do if message type cannot be determined?
                        final int typeId = message.getMessageType().isPresent() ? types.get(message.getMessageType().get()) : 0;

                        Optional<Instant> validTimeStart = Optional.empty();
                        Optional<Instant> validTimeEnd = Optional.empty();
                        if (message.getValidityTime().isPresent()) {
                            final PartialOrCompleteTimePeriod validityPeriod = message.getValidityTime().get();
                            if (validityPeriod.isCompleteStrict()) {
                                validTimeStart = Optional.of(validityPeriod.getStartTime().get().getCompleteTime().get().toInstant());
                                validTimeEnd = Optional.of(validityPeriod.getEndTime().get().getCompleteTime().get().toInstant());
                            } else {
                                // TODO Completion reference time
                                final PartialOrCompleteTimePeriod completeValidityPeriod = validityPeriod.toBuilder()
                                        .completePartialStartingNear(ZonedDateTime.now(clock))
                                        .build();
                                if (completeValidityPeriod.isCompleteStrict()) {
                                    validTimeStart = Optional.of(completeValidityPeriod.getStartTime().get().getCompleteTime().get().toInstant());
                                    validTimeEnd = Optional.of(completeValidityPeriod.getEndTime().get().getCompleteTime().get().toInstant());
                                }
                            }
                        }

                        Optional<String> version = Optional.empty();
                        if (bulletin.getHeading().getType() != BulletinHeading.Type.NORMAL) {
                            final int augmentationNumber = bulletin.getHeading()
                                    .getBulletinAugmentationNumber()
                                    .orElseThrow(() -> new IllegalStateException("Heading type is not normal but augmentation number is missing"));
                            version = Optional.of(
                                    bulletin.getHeading().getType().getPrefix() + String.valueOf(Character.toChars('A' + augmentationNumber - 1)));
                        }

                        return Message.builder()//
                                .setHeading(heading)//
                                .setIcaoAirportCode(airportCode)//
                                .setMessage(message.getOriginalMessage())//
                                .setMessageTime(issueInstant)//
                                .setRoute(routeId)//
                                .setType(typeId)//
                                .setValidFrom(validTimeStart)//
                                .setValidTo(validTimeEnd)//
                                .setNullableFileModified(fileModified)//
                                .setVersion(version)//
                                .build();
                    }).collect(Collectors.toSet());
        }
        return Collections.emptySet();
    }

}
