package fi.fmi.avi.archiver.message.populator;

import static java.util.Objects.requireNonNull;

import java.time.ZoneOffset;
import java.time.chrono.ChronoZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nullable;

import fi.fmi.avi.archiver.file.FileMetadata;
import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessageIWXXMDetails;
import fi.fmi.avi.model.GenericAviationWeatherMessage;
import fi.fmi.avi.model.GenericAviationWeatherMessage.LocationIndicatorType;
import fi.fmi.avi.model.MessageType;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.PartialOrCompleteTimePeriod;

/**
 * Populate {@link ArchiveAviationMessage.Builder} properties from message data in {@link InputAviationMessage}.
 *
 * <p>
 * Format and type are mapped from object to id by mappings provided as {@link #MessageDataPopulator(MessagePopulatorHelper, Map, Map)} constructor parameters.
 * </p>
 *
 * <p>
 * If the time properties are complete, they are used as is. Partial times are primarily completed using the file name timestamp if it exists. If the timestamp in
 * the file name is partial, it is first completed near the file modification time if it exists, otherwise near current time. Partial validity period is primarily
 * completed near the resolved message time. (See
 * {@link MessagePopulatorHelper#resolveCompleteTime(PartialOrCompleteTimeInstant, FileMetadata)} and
 * {@link MessagePopulatorHelper#tryCompletePeriod(PartialOrCompleteTimePeriod, PartialOrCompleteTimeInstant, FileMetadata)} for more information on
 * the completion algorithm.) Populated time properties are {@link ArchiveAviationMessage#getMessageTime()}, {@link ArchiveAviationMessage#getValidFrom()} and
 * {@link ArchiveAviationMessage#getValidTo()}.
 * </p>
 *
 * <p>
 * The {@link GenericAviationWeatherMessage#getLocationIndicators() location indicator} used to populate
 * {@link ArchiveAviationMessage#getIcaoAirportCode()} is chosen by looking for first existing value in order of a
 * {@link #setMessageTypeLocationIndicatorTypes(Map) message type-specific location indicator list}, if exists for message type in question, or else in order
 * of a {@link #setDefaultLocationIndicatorTypes(List) default list} of location indicator types.
 * <p>
 * {@link ArchiveAviationMessage#getMessage()} and {@link ArchiveAviationMessageIWXXMDetails#getXMLNamespace()} are also populated.
 */
public class MessageDataPopulator implements MessagePopulator {
    private static final List<LocationIndicatorType> DEFAULT_LOCATION_INDICATOR_TYPES = Collections.unmodifiableList(
            Arrays.asList(LocationIndicatorType.AERODROME, LocationIndicatorType.ISSUING_AIR_TRAFFIC_SERVICES_REGION));
    private static final Map<MessageType, List<LocationIndicatorType>> DEFAULT_MESSAGE_TYPE_LOCATION_INDICATOR_TYPES //
            = createDefaultMessageTypeLocationIndicatorTypes();
    private static final PartialOrCompleteTimePeriod EMPTY_PARTIAL_OR_COMPLETE_TIME_PERIOD = PartialOrCompleteTimePeriod.builder().build();

    private final MessagePopulatorHelper helper;
    private final Map<GenericAviationWeatherMessage.Format, Integer> formatIds;
    private final Map<MessageType, Integer> typeIds;

    private Map<MessageType, List<LocationIndicatorType>> messageTypeLocationIndicatorTypes = DEFAULT_MESSAGE_TYPE_LOCATION_INDICATOR_TYPES;
    private List<LocationIndicatorType> defaultLocationIndicatorTypes = DEFAULT_LOCATION_INDICATOR_TYPES;

    public MessageDataPopulator(final MessagePopulatorHelper helper, final Map<GenericAviationWeatherMessage.Format, Integer> formatIds,
            final Map<MessageType, Integer> typeIds) {
        this.helper = requireNonNull(helper, "helper");
        this.formatIds = requireNonNull(formatIds, "formatIds");
        this.typeIds = requireNonNull(typeIds, "typeIds");
    }

    private static Map<MessageType, List<LocationIndicatorType>> createDefaultMessageTypeLocationIndicatorTypes() {
        final Map<MessageType, List<LocationIndicatorType>> builder = new HashMap<>();
        put(builder, MessageType.AIRMET, LocationIndicatorType.ISSUING_AIR_TRAFFIC_SERVICES_REGION);
        put(builder, MessageType.METAR, LocationIndicatorType.AERODROME);
        put(builder, MessageType.SIGMET, LocationIndicatorType.ISSUING_AIR_TRAFFIC_SERVICES_REGION);
        put(builder, MessageType.SPECI, LocationIndicatorType.AERODROME);
        put(builder, MessageType.SPACE_WEATHER_ADVISORY);
        put(builder, MessageType.TAF, LocationIndicatorType.AERODROME);
        put(builder, MessageType.TROPICAL_CYCLONE_ADVISORY);
        put(builder, MessageType.VOLCANIC_ASH_ADVISORY);
        return Collections.unmodifiableMap(builder);
    }

    private static <K, V> void put(final Map<K, List<V>> map, final K key) {
        map.put(key, Collections.emptyList());
    }

    private static <K, V> void put(final Map<K, List<V>> map, final K key, final V value) {
        map.put(key, Collections.singletonList(value));
    }

    @Override
    public void populate(final InputAviationMessage input, final ArchiveAviationMessage.Builder builder) {
        requireNonNull(input, "input");
        requireNonNull(builder, "builder");
        final GenericAviationWeatherMessage inputMessage = input.getMessage();
        Optional.ofNullable(formatIds.get(inputMessage.getMessageFormat()))//
                .ifPresent(builder::setFormat);
        inputMessage.getMessageType()//
                .map(typeIds::get)//
                .ifPresent(builder::setType);
        inputMessage.getIssueTime()//
                .flatMap(issueTime -> helper.resolveCompleteTime(issueTime, input.getFileMetadata()))//
                .map(ChronoZonedDateTime::toInstant)//
                .ifPresent(builder::setMessageTime);
        getLocationIndicator(inputMessage.getMessageType().orElse(null), inputMessage.getLocationIndicators())//
                .ifPresent(builder::setIcaoAirportCode);
        final PartialOrCompleteTimePeriod validityTime = inputMessage.getValidityTime()//
                .map(period -> helper.tryCompletePeriod(period, getNullablePartialOrCompleteMessageTime(builder, inputMessage), input.getFileMetadata()))//
                .orElse(EMPTY_PARTIAL_OR_COMPLETE_TIME_PERIOD);
        validityTime.getStartTime()//
                .flatMap(PartialOrCompleteTimeInstant::getCompleteTime)//
                .map(ChronoZonedDateTime::toInstant)//
                .ifPresent(builder::setValidFrom);
        validityTime.getEndTime()//
                .flatMap(PartialOrCompleteTimeInstant::getCompleteTime)//
                .map(ChronoZonedDateTime::toInstant)//
                .ifPresent(builder::setValidTo);
        inputMessage.getXMLNamespace()//
                .ifPresent(xmlNamespace -> builder.mutateIWXXMDetails(iwxxmDetails -> iwxxmDetails.setXMLNamespace(xmlNamespace)));
        builder.setMessage(inputMessage.getOriginalMessage());
    }

    @Nullable
    private PartialOrCompleteTimeInstant getNullablePartialOrCompleteMessageTime(final ArchiveAviationMessage.Builder builder,
            final GenericAviationWeatherMessage inputMessage) {
        return MessagePopulatorHelper.tryGet(builder, ArchiveAviationMessage.Builder::getMessageTime)//
                .map(messageTime -> PartialOrCompleteTimeInstant.of(messageTime.atZone(ZoneOffset.UTC)))//
                .orElse(inputMessage.getIssueTime().orElse(null));
    }

    private Optional<String> getLocationIndicator(@Nullable final MessageType messageType, final Map<LocationIndicatorType, String> locationIndicators) {
        if (locationIndicators.isEmpty()) {
            return Optional.empty();
        }
        return getLocationIndicatorTypes(messageType).stream()//
                .map(locationIndicators::get)//
                .filter(Objects::nonNull)//
                .findFirst();
    }

    private List<LocationIndicatorType> getLocationIndicatorTypes(final @Nullable MessageType messageType) {
        final List<LocationIndicatorType> types = messageTypeLocationIndicatorTypes.get(messageType);
        return types != null ? types : defaultLocationIndicatorTypes;
    }

    /**
     * Set a custom location indicator type preference order for specified message types.
     * See {@link MessageDataPopulator class documentation} for overall description of selecting the location indicator.
     *
     * <p>
     * The default mappings are
     * </p>
     * <ul>
     *     <li>METAR, SPECI and TAF:</li>
     *     <ol>
     *         <li>{@link LocationIndicatorType#AERODROME}</li>
     *     </ol>
     *     <li>AIRMET and SIGMET:</li>
     *     <ol>
     *         <li>{@link LocationIndicatorType#ISSUING_AIR_TRAFFIC_SERVICES_REGION}</li>
     *     </ol>
     *     <li>Space weather advisory, Tropical cyclone advisory and Volcanic ash advisory:</li>
     *     <ol>
     *         <li>An empty list (omitting location indicator completely)</li>
     *     </ol>
     * </ul>
     *
     * @param messageTypeLocationIndicatorTypes
     *         location indicator types for specified message types
     */
    public void setMessageTypeLocationIndicatorTypes(final Map<MessageType, List<LocationIndicatorType>> messageTypeLocationIndicatorTypes) {
        this.messageTypeLocationIndicatorTypes = requireNonNull(messageTypeLocationIndicatorTypes, "messageTypeLocationIndicators");
    }

    /**
     * Set the location indicator type preference order for message types not {@link #setMessageTypeLocationIndicatorTypes(Map) explicitly configured}.
     * See {@link MessageDataPopulator class documentation} for overall description of selecting the location indicator.
     *
     * <p>
     * The default list is
     * </p>
     * <ol>
     *     <li>{@link LocationIndicatorType#AERODROME}</li>
     *     <li>{@link LocationIndicatorType#ISSUING_AIR_TRAFFIC_SERVICES_REGION}</li>
     * </ol>
     *
     * @param defaultLocationIndicatorTypes
     *         default location indicator type preference list
     */
    public void setDefaultLocationIndicatorTypes(final List<LocationIndicatorType> defaultLocationIndicatorTypes) {
        this.defaultLocationIndicatorTypes = requireNonNull(defaultLocationIndicatorTypes, "locationIndicatorOrderOfPreference");
    }
}
