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
 * If time properties are complete, they are used as is. Partial times are completed primarily near to timestamp in file name, if exists. If timestamp in
 * file name is partial, it is first completed near to file modification time if exists, or current clock time. Partial validity period is primarily
 * completed near to resolved message time. (See
 * {@link MessagePopulatorHelper#resolveCompleteTime(PartialOrCompleteTimeInstant, FileMetadata)} and
 * {@link MessagePopulatorHelper#tryCompletePeriod(PartialOrCompleteTimePeriod, PartialOrCompleteTimeInstant, FileMetadata)} for more information on
 * completion algorithm.) Populated time properties are {@link ArchiveAviationMessage#getMessageTime()}, {@link ArchiveAviationMessage#getValidFrom()} and
 * {@link ArchiveAviationMessage#getValidTo()}.
 * </p>
 *
 * <p>
 * The {@link GenericAviationWeatherMessage#getLocationIndicators() location indicator} used to populate the
 * {@link ArchiveAviationMessage#getIcaoAirportCode()} is chosen using the first match in a configured list of location indicator types. Multiple lists may
 * be configured for each message type. The list of location indicator types is chosen by following rules:
 * </p>
 * <ol>
 *     <li>The list of location indicator types is read from {@link #setMessageTypeLocationIndicatorTypes(Map)} map, if it has been set and an entry with
 *     message type in question exists.</li>
 *     <li>If {@link #setForceCustomMessageTypeLocationIndicatorTypes(boolean)} is {@code false}, the default map indexed by message types is
 *     searched. (See {@link #setMessageTypeLocationIndicatorTypes(Map)}.)</li>
 *     <li>If no match by message type is found, the default list {@link #setDefaultLocationIndicatorTypes(List)} is used.</li>
 * </ol>
 *
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

    private Map<MessageType, List<LocationIndicatorType>> messageTypeLocationIndicatorTypes = Collections.emptyMap();
    private boolean forceCustomMessageTypeLocationIndicatorTypes; // = false;
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
        List<LocationIndicatorType> types = messageTypeLocationIndicatorTypes.get(messageType);
        if (types != null) {
            return types;
        }
        if (!forceCustomMessageTypeLocationIndicatorTypes) {
            types = DEFAULT_MESSAGE_TYPE_LOCATION_INDICATOR_TYPES.get(messageType);
            if (types != null) {
                return types;
            }
        }
        return defaultLocationIndicatorTypes;
    }

    /**
     * Set a custom location indicator type preference order for specified message types.
     * See {@link MessageDataPopulator class documentation} for overall description of selecting the location indicator. The provided map is effectively merged
     * with the default configuration, unless {@link #setForceCustomMessageTypeLocationIndicatorTypes(boolean)} is set to {@code true}.
     *
     * <p>
     * The default mappings are
     * </p>
     * <ul>
     *     <li>{@link LocationIndicatorType#AERODROME} for METAR, SPECI and TAF</li>
     *     <li>{@link LocationIndicatorType#ISSUING_AIR_TRAFFIC_SERVICES_REGION} for AIRMET and SIGMET</li>
     *     <li><strong>Omit</strong> completely for Space weather advisory, Tropical cyclone advisory and Volcanic ash advisory</li>
     * </ul>
     *
     * @param messageTypeLocationIndicatorTypes
     *         location indicator types for specified message types
     */
    public void setMessageTypeLocationIndicatorTypes(final Map<MessageType, List<LocationIndicatorType>> messageTypeLocationIndicatorTypes) {
        this.messageTypeLocationIndicatorTypes = requireNonNull(messageTypeLocationIndicatorTypes, "messageTypeLocationIndicators");
    }

    /**
     * Specify, whether {@link #setMessageTypeLocationIndicatorTypes(Map)} shall override completely default message type specific configuration.
     * If set to {@code true}, default mappings are overridden and location indicator types for message types missing from custom map are resolved using the
     * {@link #setDefaultLocationIndicatorTypes(List)} list. If set to {@code false}, custom configuration is effectively merged with default mappings,
     * meaning that message types not specified in custom configuration will use default configuration when exists.
     *
     * <p>
     * The default value is {@code false}.
     * </p>
     *
     * @param forceCustomMessageTypeLocationIndicatorTypes
     *         whether to force use of custom message type location indicator types
     */
    public void setForceCustomMessageTypeLocationIndicatorTypes(final boolean forceCustomMessageTypeLocationIndicatorTypes) {
        this.forceCustomMessageTypeLocationIndicatorTypes = forceCustomMessageTypeLocationIndicatorTypes;
    }

    /**
     * Set the location indicator type preference order for message types not explicitly specified in configuration.
     *
     * <p>
     * The default list is
     * </p>
     * <ul>
     *     <li>{@link LocationIndicatorType#AERODROME}</li>
     *     <li>{@link LocationIndicatorType#ISSUING_AIR_TRAFFIC_SERVICES_REGION}</li>
     * </ul>
     *
     * @param defaultLocationIndicatorTypes
     *         default location indicator type preference list
     */
    public void setDefaultLocationIndicatorTypes(final List<LocationIndicatorType> defaultLocationIndicatorTypes) {
        this.defaultLocationIndicatorTypes = requireNonNull(defaultLocationIndicatorTypes, "locationIndicatorOrderOfPreference");
    }
}
