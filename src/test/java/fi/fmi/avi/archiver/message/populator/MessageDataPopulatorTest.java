package fi.fmi.avi.archiver.message.populator;

import static fi.fmi.avi.archiver.message.populator.MessagePopulatorTests.EMPTY_RESULT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.annotation.Nullable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.converter.ArgumentConversionException;
import org.junit.jupiter.params.converter.ConvertWith;
import org.junit.jupiter.params.converter.SimpleArgumentConverter;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.junit.jupiter.params.provider.EnumSource;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import fi.fmi.avi.archiver.file.FileMetadata;
import fi.fmi.avi.archiver.file.FileReference;
import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.model.GenericAviationWeatherMessage;
import fi.fmi.avi.model.GenericAviationWeatherMessage.LocationIndicatorType;
import fi.fmi.avi.model.MessageType;
import fi.fmi.avi.model.PartialDateTime;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.PartialOrCompleteTimePeriod;
import fi.fmi.avi.model.immutable.GenericAviationWeatherMessageImpl;

class MessageDataPopulatorTest {
    private static final InputAviationMessage INPUT_MESSAGE_TEMPLATE = InputAviationMessage.builder()//
            .setFileMetadata(FileMetadata.builder()//
                    .setFileReference(FileReference.create("testproduct", "taf.txt"))//
                    .setFileModified(Instant.parse("2000-01-02T03:05:34Z"))//
                    .mutateFileConfig(fileConfig -> fileConfig//
                            .setFormat(MessagePopulatorTests.FormatId.TAC.getFormat())//
                            .setFormatId(MessagePopulatorTests.FormatId.TAC.getId())//
                            .setPattern(MessagePopulatorTests.FILE_NAME_PATTERN)//
                            .setNameTimeZone(ZoneOffset.UTC)))//
            .setMessage(GenericAviationWeatherMessageImpl.builder()//
                    .setTranslated(false)//
                    .setMessageFormat(GenericAviationWeatherMessage.Format.TAC)//
                    .setOriginalMessage("")//
                    .build())//
            .buildPartial();

    private MessageDataPopulator populator;

    private static Optional<PartialOrCompleteTimeInstant> partialOrCompleteTimeInstant(@Nullable final PartialDateTime partialTime,
            @Nullable final ZonedDateTime completeTime) {
        if (partialTime == null && completeTime == null) {
            return Optional.empty();
        }
        return Optional.of(PartialOrCompleteTimeInstant.builder()//
                .setNullablePartialTime(partialTime)//
                .setNullableCompleteTime(completeTime)//
                .build());
    }

    @BeforeEach
    void setUp() {
        populator = newPopulator(Clock.systemUTC());
    }

    private MessageDataPopulator newPopulator(final Clock clock) {
        return newPopulator(new MessagePopulatorHelper(clock));
    }

    private MessageDataPopulator newPopulator(final MessagePopulatorHelper helper) {
        return new MessageDataPopulator(helper, MessagePopulatorTests.FORMAT_IDS, MessagePopulatorTests.TYPE_IDS);
    }

    @Test
    void populates_only_mandatory_values_when_other_message_information_is_not_available() {
        final MessagePopulatingContext context = TestMessagePopulatingContext.create(INPUT_MESSAGE_TEMPLATE);
        final ArchiveAviationMessage expected = ArchiveAviationMessage.builder()//
                .setMessage(INPUT_MESSAGE_TEMPLATE.getMessage().getOriginalMessage())//
                .setFormat(MessagePopulatorTests.FormatId.valueOf(INPUT_MESSAGE_TEMPLATE.getMessage().getMessageFormat()).getId())//
                .buildPartial();

        final ArchiveAviationMessage.Builder builder = EMPTY_RESULT.toBuilder();
        populator.populate(context, builder);
        assertThat(builder.buildPartial()).isEqualTo(expected);
    }

    @ParameterizedTest
    @EnumSource(MessagePopulatorTests.FormatId.class)
    void populates_format(final MessagePopulatorTests.FormatId expectedFormat) {
        final InputAviationMessage inputMessage = INPUT_MESSAGE_TEMPLATE.toBuilder()//
                .mapMessage(message -> GenericAviationWeatherMessageImpl.Builder.from(message)//
                        .setMessageFormat(expectedFormat.getFormat())//
                        .build())//
                .buildPartial();
        final MessagePopulatingContext context = TestMessagePopulatingContext.create(inputMessage);

        final ArchiveAviationMessage.Builder builder = EMPTY_RESULT.toBuilder();
        populator.populate(context, builder);
        assertThat(builder.getFormat()).isEqualTo(expectedFormat.getId());
    }

    @ParameterizedTest
    @EnumSource(MessagePopulatorTests.TypeId.class)
    void populates_type_when_exists(final MessagePopulatorTests.TypeId expectedType) {
        final InputAviationMessage inputMessage = INPUT_MESSAGE_TEMPLATE.toBuilder()//
                .mapMessage(message -> GenericAviationWeatherMessageImpl.Builder.from(message)//
                        .setMessageType(expectedType.getType())//
                        .build())//
                .buildPartial();
        final MessagePopulatingContext context = TestMessagePopulatingContext.create(inputMessage);

        final ArchiveAviationMessage.Builder builder = EMPTY_RESULT.toBuilder();
        populator.populate(context, builder);
        assertThat(builder.getType()).isEqualTo(expectedType.getId());
    }

    @ParameterizedTest
    @CsvFileSource(resources = "MessageDataPopulatorTest_populates_messageTime_when_exists.csv", numLinesToSkip = 1)
    void populates_messageTime_when_exists(@Nullable final PartialDateTime partialIssueTime, @Nullable final ZonedDateTime completeIssueTime,
            final String filename, @Nullable final Instant fileModified, final ZonedDateTime clock, final Instant expectedTime) {
        populator = newPopulator(Clock.fixed(clock.toInstant(), clock.getZone()));
        final InputAviationMessage inputMessage = INPUT_MESSAGE_TEMPLATE.toBuilder()//
                .mutateFileMetadata(filedata -> filedata.mutateFileReference(ref -> ref.setFilename(filename))//
                        .setNullableFileModified(fileModified))//
                .mapMessage(message -> GenericAviationWeatherMessageImpl.Builder.from(message)//
                        .setIssueTime(PartialOrCompleteTimeInstant.builder()//
                                .setNullablePartialTime(partialIssueTime)//
                                .setNullableCompleteTime(completeIssueTime)//
                                .build())//
                        .build())//
                .buildPartial();
        final MessagePopulatingContext context = TestMessagePopulatingContext.create(inputMessage);

        final ArchiveAviationMessage.Builder builder = EMPTY_RESULT.toBuilder();
        populator.populate(context, builder);
        assertThat(builder.getMessageTime()).isEqualTo(expectedTime);
    }

    @ParameterizedTest
    @CsvFileSource(resources = "MessageDataPopulatorTest_populates_stationIcaoCode_when_exists.csv", numLinesToSkip = 1)
    void populates_stationIcaoCode_when_exists(final PopulatorConfig populatorConfig, final MessagePopulatorTests.TypeId messageType,
            @ConvertWith(ToLocationIndicatorMap.class) final Map<LocationIndicatorType, String> locationIndicators,
            @Nullable final String expectedStationIcaoCode) {
        populatorConfig.accept(populator);
        final InputAviationMessage inputMessage = INPUT_MESSAGE_TEMPLATE.toBuilder()//
                .mapMessage(message -> GenericAviationWeatherMessageImpl.Builder.from(message)//
                        .setMessageType(messageType.getType())//
                        .putAllLocationIndicators(locationIndicators)//
                        .build())//
                .buildPartial();
        final MessagePopulatingContext context = TestMessagePopulatingContext.create(inputMessage);

        final String initialStationIcaoCode = "NULL";
        final ArchiveAviationMessage.Builder builder = EMPTY_RESULT.toBuilder()//
                .setStationIcaoCode(initialStationIcaoCode);
        populator.populate(context, builder);
        assertThat(builder.getStationIcaoCode()).isEqualTo(Optional.ofNullable(expectedStationIcaoCode).orElse(initialStationIcaoCode));
    }

    @ParameterizedTest
    @CsvFileSource(resources = "MessageDataPopulatorTest_populates_stationIcaoCode_when_exists.csv", numLinesToSkip = 1)
    void populates_stationIcaoCode_when_exists_and_message_type_is_already_set(final PopulatorConfig populatorConfig,
            final MessagePopulatorTests.TypeId messageType,
            @ConvertWith(ToLocationIndicatorMap.class) final Map<LocationIndicatorType, String> locationIndicators,
            @Nullable final String expectedStationIcaoCode) {
        populatorConfig.accept(populator);
        final InputAviationMessage inputMessage = INPUT_MESSAGE_TEMPLATE.toBuilder()//
                .mapMessage(message -> GenericAviationWeatherMessageImpl.Builder.from(message)//
                        // Omitting message type
                        .putAllLocationIndicators(locationIndicators)//
                        .build())//
                .buildPartial();
        final MessagePopulatingContext context = TestMessagePopulatingContext.create(inputMessage);

        final String initialStationIcaoCode = "NULL";
        final ArchiveAviationMessage.Builder builder = EMPTY_RESULT.toBuilder()//
                .setType(messageType.getId())//
                .setStationIcaoCode(initialStationIcaoCode);
        populator.populate(context, builder);
        assertThat(builder.getStationIcaoCode()).isEqualTo(Optional.ofNullable(expectedStationIcaoCode).orElse(initialStationIcaoCode));
    }

    @ParameterizedTest
    @CsvFileSource(resources = "MessageDataPopulatorTest_populates_validFrom_and_validTo_when_exists.csv", numLinesToSkip = 1)
    void populates_validFrom_and_validTo_when_exists(//
            @Nullable final PartialDateTime partialStartTime, @Nullable final ZonedDateTime completeStartTime, //
            @Nullable final PartialDateTime partialEndTime, @Nullable final ZonedDateTime completeEndTime, //
            @Nullable final PartialDateTime partialPrimaryReference, @Nullable final ZonedDateTime completePrimaryReference, //
            final String filename, @Nullable final Instant fileModified, final ZonedDateTime clock, //
            @Nullable final Instant expectedValidFrom, @Nullable final Instant expectedValidTo) {
        populator = newPopulator(Clock.fixed(clock.toInstant(), clock.getZone()));
        final InputAviationMessage inputMessage = INPUT_MESSAGE_TEMPLATE.toBuilder()//
                .mutateFileMetadata(fileData -> fileData.mutateFileReference(ref -> ref.setFilename(filename))//
                        .setNullableFileModified(fileModified))//
                .mapMessage(message -> GenericAviationWeatherMessageImpl.Builder.from(message)//
                        .setIssueTime(partialOrCompleteTimeInstant(partialPrimaryReference, completePrimaryReference))//
                        .setValidityTime(PartialOrCompleteTimePeriod.builder()//
                                .setStartTime(partialOrCompleteTimeInstant(partialStartTime, completeStartTime))//
                                .setEndTime(partialOrCompleteTimeInstant(partialEndTime, completeEndTime))//
                                .build())//
                        .build())//
                .buildPartial();
        final MessagePopulatingContext context = TestMessagePopulatingContext.create(inputMessage);

        final Instant initialValidFrom = Instant.EPOCH;
        final Instant initialValidTo = initialValidFrom.plus(1, ChronoUnit.DAYS);
        final ArchiveAviationMessage.Builder builder = EMPTY_RESULT.toBuilder()//
                .setValidFrom(initialValidFrom)//
                .setValidTo(initialValidTo);
        populator.populate(context, builder);
        assertSoftly(softly -> {
            softly.assertThat(builder.getValidFrom()).as("validFrom").hasValue(Optional.ofNullable(expectedValidFrom).orElse(initialValidFrom));
            softly.assertThat(builder.getValidTo()).as("validTo").hasValue(Optional.ofNullable(expectedValidTo).orElse(initialValidTo));
        });
    }

    @ParameterizedTest
    @CsvFileSource(resources = "MessageDataPopulatorTest_populates_validFrom_and_validTo_when_exists.csv", numLinesToSkip = 1)
    void populates_validFrom_and_validTo_when_exists_referencing_already_set_message_time(//
            @Nullable final PartialDateTime partialStartTime, @Nullable final ZonedDateTime completeStartTime, //
            @Nullable final PartialDateTime partialEndTime, @Nullable final ZonedDateTime completeEndTime, //
            @Nullable final PartialDateTime partialPrimaryReference, @Nullable final ZonedDateTime completePrimaryReference, //
            final String filename, @Nullable final Instant fileModified, final ZonedDateTime clock, //
            @Nullable final Instant expectedValidFrom, @Nullable final Instant expectedValidTo) {
        final MessagePopulatorHelper helper = new MessagePopulatorHelper(Clock.fixed(clock.toInstant(), clock.getZone()));
        populator = newPopulator(helper);
        final InputAviationMessage inputMessage = INPUT_MESSAGE_TEMPLATE.toBuilder()//
                .mutateFileMetadata(fileData -> fileData.mutateFileReference(ref -> ref.setFilename(filename))//
                        .setNullableFileModified(fileModified))//
                .mapMessage(message -> GenericAviationWeatherMessageImpl.Builder.from(message)//
                        // Omitting issue time
                        .setValidityTime(PartialOrCompleteTimePeriod.builder()//
                                .setStartTime(partialOrCompleteTimeInstant(partialStartTime, completeStartTime))//
                                .setEndTime(partialOrCompleteTimeInstant(partialEndTime, completeEndTime))//
                                .build())//
                        .build())//
                .buildPartial();
        final MessagePopulatingContext context = TestMessagePopulatingContext.create(inputMessage);

        final Instant initialValidFrom = Instant.EPOCH;
        final Instant initialValidTo = initialValidFrom.plus(1, ChronoUnit.DAYS);
        @Nullable
        final ZonedDateTime initialMessageTime = partialOrCompleteTimeInstant(partialPrimaryReference, completePrimaryReference)//
                .flatMap(time -> helper.resolveCompleteTime(time, inputMessage.getFileMetadata()))//
                .orElse(null);
        final ArchiveAviationMessage.Builder builder = EMPTY_RESULT.toBuilder()//
                .setValidFrom(initialValidFrom)//
                .setValidTo(initialValidTo);
        if (initialMessageTime != null) {
            builder.setMessageTime(initialMessageTime.toInstant());
        }
        populator.populate(context, builder);
        assertSoftly(softly -> {
            softly.assertThat(builder.getValidFrom()).as("validFrom").hasValue(Optional.ofNullable(expectedValidFrom).orElse(initialValidFrom));
            softly.assertThat(builder.getValidTo()).as("validTo").hasValue(Optional.ofNullable(expectedValidTo).orElse(initialValidTo));
        });
    }

    @Test
    void populates_xmlNamespace_when_exists() {
        final String expectedNamespace = "http://expected/namespace";
        final InputAviationMessage inputMessage = INPUT_MESSAGE_TEMPLATE.toBuilder()//
                .mapMessage(message -> GenericAviationWeatherMessageImpl.Builder.from(message)//
                        .setXMLNamespace(expectedNamespace)//
                        .build())//
                .buildPartial();
        final MessagePopulatingContext context = TestMessagePopulatingContext.create(inputMessage);

        final ArchiveAviationMessage.Builder builder = EMPTY_RESULT.toBuilder();
        populator.populate(context, builder);
        assertThat(builder.getIWXXMDetailsBuilder().getXMLNamespace()).isEqualTo(Optional.of(expectedNamespace));
    }

    @Test
    void populates_originalMessage_when_exists() {
        final String expectedMessage = "This is the original message";
        final InputAviationMessage inputMessage = INPUT_MESSAGE_TEMPLATE.toBuilder()//
                .mapMessage(message -> GenericAviationWeatherMessageImpl.Builder.from(message)//
                        .setOriginalMessage(expectedMessage)//
                        .build())//
                .buildPartial();
        final MessagePopulatingContext context = TestMessagePopulatingContext.create(inputMessage);

        final ArchiveAviationMessage.Builder builder = EMPTY_RESULT.toBuilder();
        populator.populate(context, builder);
        assertThat(builder.getMessage()).isEqualTo(expectedMessage);
    }

    enum PopulatorConfig implements Consumer<MessageDataPopulator> {
        DEFAULT {
            @Override
            public void accept(final MessageDataPopulator populator) {
            }
        }, //
        CUSTOM_MESSAGE_TYPE_SPECIFIC_LOCATION_INDICATORS {
            @Override
            public void accept(final MessageDataPopulator populator) {
                populator.setMessageTypeLocationIndicatorTypes(ALT_MESSAGE_TYPE_LOCATION_INDICATOR_TYPES);
            }
        }, //
        CUSTOM_GLOBAL_LOCATION_INDICATORS {
            @Override
            public void accept(final MessageDataPopulator populator) {
                populator.setDefaultLocationIndicatorTypes(ALT_DEFAULT_LOCATION_INDICATORS);
            }
        }, //
        ALL_LOCATION_INDICATOR_TYPES_AS_GLOBAL_DEFAULTS {
            @Override
            public void accept(final MessageDataPopulator populator) {
                populator.setDefaultLocationIndicatorTypes(Collections.unmodifiableList(Arrays.asList(LocationIndicatorType.values())));
            }
        }, //
        ONLY_GLOBAL_LOCATION_INDICATORS {
            @Override
            public void accept(final MessageDataPopulator populator) {
                populator.setMessageTypeLocationIndicatorTypes(Collections.emptyMap());
            }
        }, //
        NO_LOCATION_INDICATORS {
            @Override
            public void accept(final MessageDataPopulator populator) {
                populator.setMessageTypeLocationIndicatorTypes(Collections.emptyMap());
                populator.setDefaultLocationIndicatorTypes(Collections.emptyList());
            }
        }, //
        ;

        private static final Map<MessageType, List<LocationIndicatorType>> ALT_MESSAGE_TYPE_LOCATION_INDICATOR_TYPES = ImmutableMap.<MessageType, List<LocationIndicatorType>> builder()//
                .put(MessageType.AIRMET, ImmutableList.of(LocationIndicatorType.ORIGINATING_METEOROLOGICAL_WATCH_OFFICE,
                        LocationIndicatorType.ISSUING_AIR_TRAFFIC_SERVICES_UNIT))//
                .put(MessageType.SIGMET, ImmutableList.of())//
                .put(MessageType.TROPICAL_CYCLONE_ADVISORY, ImmutableList.of())//
                .put(MessageType.VOLCANIC_ASH_ADVISORY, ImmutableList.of(LocationIndicatorType.ORIGINATING_METEOROLOGICAL_WATCH_OFFICE,
                        LocationIndicatorType.ISSUING_AIR_TRAFFIC_SERVICES_UNIT))//
                .build();
        private static final List<LocationIndicatorType> ALT_DEFAULT_LOCATION_INDICATORS = Arrays.asList(//
                LocationIndicatorType.AERODROME, //
                LocationIndicatorType.ORIGINATING_METEOROLOGICAL_WATCH_OFFICE, //
                LocationIndicatorType.ISSUING_CENTRE);

        @Override
        public abstract void accept(final MessageDataPopulator populator);
    }

    static final class ToLocationIndicatorMap extends SimpleArgumentConverter {
        private static final Map<String, LocationIndicatorType> INDICATORS_TO_TYPE = ImmutableMap.<String, LocationIndicatorType> builder()//
                .put("AERO", LocationIndicatorType.AERODROME)//
                .put("CENT", LocationIndicatorType.ISSUING_CENTRE)//
                .put("UNIT", LocationIndicatorType.ISSUING_AIR_TRAFFIC_SERVICES_UNIT)
                .put("REGI", LocationIndicatorType.ISSUING_AIR_TRAFFIC_SERVICES_REGION)
                .put("OFFI", LocationIndicatorType.ORIGINATING_METEOROLOGICAL_WATCH_OFFICE)
                .build();

        @Override
        protected Object convert(final Object source, final Class<?> targetType) throws ArgumentConversionException {
            if (!targetType.isAssignableFrom(Map.class)) {
                throw new ArgumentConversionException("Can only convert to type " + Map.class);
            }
            if (source == null) {
                return ImmutableMap.of();
            }
            return Arrays.stream(source.toString().trim().split("\\s+"))//
                    .filter(indicator -> !indicator.isEmpty())//
                    .collect(ImmutableMap.toImmutableMap(INDICATORS_TO_TYPE::get, Function.identity()));
        }
    }
}
