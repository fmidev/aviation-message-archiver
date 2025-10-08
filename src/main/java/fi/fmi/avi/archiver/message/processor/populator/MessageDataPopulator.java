package fi.fmi.avi.archiver.message.processor.populator;

import fi.fmi.avi.archiver.file.FileMetadata;
import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage.Builder;
import fi.fmi.avi.archiver.message.ArchiveAviationMessageIWXXMDetails;
import fi.fmi.avi.archiver.message.processor.MessageProcessorContext;
import fi.fmi.avi.model.GenericAviationWeatherMessage;
import fi.fmi.avi.model.GenericAviationWeatherMessage.LocationIndicatorType;
import fi.fmi.avi.model.MessageType;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.PartialOrCompleteTimePeriod;

import javax.annotation.Nullable;
import java.time.ZoneOffset;
import java.time.chrono.ChronoZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static fi.fmi.avi.archiver.message.processor.MessageProcessorHelper.tryGet;
import static java.util.Objects.requireNonNull;

/**
 * Populate {@link Builder} properties from message data in {@link InputAviationMessage}.
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
 * {@link ArchiveAviationMessage#getStationIcaoCode()} is chosen by looking for first existing value in order of a
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

    private Map<Integer, List<LocationIndicatorType>> messageTypeLocationIndicatorTypes;
    private List<LocationIndicatorType> defaultLocationIndicatorTypes = DEFAULT_LOCATION_INDICATOR_TYPES;

    public MessageDataPopulator(final MessagePopulatorHelper helper, final Map<GenericAviationWeatherMessage.Format, Integer> formatIds,
                                final Map<MessageType, Integer> typeIds) {
        this.helper = requireNonNull(helper, "helper");
        this.formatIds = requireNonNull(formatIds, "formatIds");
        this.typeIds = requireNonNull(typeIds, "typeIds");
        setMessageTypeLocationIndicatorTypes(DEFAULT_MESSAGE_TYPE_LOCATION_INDICATOR_TYPES);
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
    public void populate(final MessageProcessorContext context, final Builder target) {
        requireNonNull(context, "context");
        requireNonNull(target, "target");
        final InputAviationMessage input = context.getInputMessage();
        final GenericAviationWeatherMessage inputMessage = input.getMessage();
        Optional.ofNullable(formatIds.get(inputMessage.getMessageFormat()))//
                .ifPresent(target::setFormat);
        inputMessage.getMessageType()//
                .map(typeIds::get)//
                .ifPresent(target::setType);
        inputMessage.getIssueTime()//
                .flatMap(issueTime -> helper.resolveCompleteTime(issueTime, input.getFileMetadata()))//
                .map(ChronoZonedDateTime::toInstant)//
                .ifPresent(target::setMessageTime);
        // Note invocation order: message type is already set before getLocationIndicator(Builder.getType(), ...)
        getLocationIndicator(tryGet(target, Builder::getType).orElse(Integer.MIN_VALUE), inputMessage.getLocationIndicators())//
                .ifPresent(target::setStationIcaoCode);
        final PartialOrCompleteTimePeriod validityTime = inputMessage.getValidityTime()//
                .map(period -> helper.tryCompletePeriod(period, getNullablePartialOrCompleteMessageTime(target, inputMessage), input.getFileMetadata()))//
                .orElse(EMPTY_PARTIAL_OR_COMPLETE_TIME_PERIOD);
        validityTime.getStartTime()//
                .flatMap(PartialOrCompleteTimeInstant::getCompleteTime)//
                .map(ChronoZonedDateTime::toInstant)//
                .ifPresent(target::setValidFrom);
        validityTime.getEndTime()//
                .flatMap(PartialOrCompleteTimeInstant::getCompleteTime)//
                .map(ChronoZonedDateTime::toInstant)//
                .ifPresent(target::setValidTo);
        inputMessage.getXMLNamespace()//
                .ifPresent(xmlNamespace -> target.mutateIWXXMDetails(iwxxmDetails -> iwxxmDetails.setXMLNamespace(xmlNamespace)));
        target.setMessage(inputMessage.getOriginalMessage());
    }

    @Nullable
    private PartialOrCompleteTimeInstant getNullablePartialOrCompleteMessageTime(final Builder builder, final GenericAviationWeatherMessage inputMessage) {
        return tryGet(builder, Builder::getMessageTime)//
                .map(messageTime -> PartialOrCompleteTimeInstant.of(messageTime.atZone(ZoneOffset.UTC)))//
                .orElse(inputMessage.getIssueTime().orElse(null));
    }

    private Optional<String> getLocationIndicator(final int messageTypeId, final Map<LocationIndicatorType, String> locationIndicators) {
        if (locationIndicators.isEmpty()) {
            return Optional.empty();
        }
        return getLocationIndicatorTypes(messageTypeId).stream()//
                .map(locationIndicators::get)//
                .filter(Objects::nonNull)//
                .findFirst();
    }

    private List<LocationIndicatorType> getLocationIndicatorTypes(final int messageTypeId) {
        final List<LocationIndicatorType> types = messageTypeLocationIndicatorTypes.get(messageTypeId);
        return types == null ? defaultLocationIndicatorTypes : types;
    }

    /**
     * Set a custom location indicator type preference order for specified message types.
     * See {@link MessageDataPopulator class documentation} for overall description of selecting the location indicator.
     *
     * <p>
     * The default mappings are
     * </p>
     * <ul>
     *     <li>METAR, SPECI and TAF:
     *         <ol>
     *             <li>{@link LocationIndicatorType#AERODROME}</li>
     *         </ol>
     *     </li>
     *     <li>AIRMET and SIGMET:
     *         <ol>
     *             <li>{@link LocationIndicatorType#ISSUING_AIR_TRAFFIC_SERVICES_REGION}</li>
     *         </ol>
     *     </li>
     *     <li>Space weather advisory, Tropical cyclone advisory and Volcanic ash advisory:
     *         <ol>
     *             <li>An empty list (omitting location indicator completely)</li>
     *         </ol>
     *     </li>
     * </ul>
     *
     * @param messageTypeLocationIndicatorTypes location indicator types for specified message types
     */
    public void setMessageTypeLocationIndicatorTypes(final Map<MessageType, List<LocationIndicatorType>> messageTypeLocationIndicatorTypes) {
        requireNonNull(messageTypeLocationIndicatorTypes, "messageTypeLocationIndicators");
        this.messageTypeLocationIndicatorTypes = Collections.unmodifiableMap(messageTypeLocationIndicatorTypes.entrySet().stream()//
                .filter(entry -> typeIds.containsKey(entry.getKey()))//
                .collect(Collectors.toMap(entry -> typeIds.get(entry.getKey()), Map.Entry::getValue)));
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
     * @param defaultLocationIndicatorTypes default location indicator type preference list
     */
    public void setDefaultLocationIndicatorTypes(final List<LocationIndicatorType> defaultLocationIndicatorTypes) {
        this.defaultLocationIndicatorTypes = requireNonNull(defaultLocationIndicatorTypes, "locationIndicatorOrderOfPreference");
    }
}
