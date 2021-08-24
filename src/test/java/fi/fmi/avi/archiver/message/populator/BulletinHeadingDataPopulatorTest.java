package fi.fmi.avi.archiver.message.populator;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import fi.fmi.avi.archiver.file.FileMetadata;
import fi.fmi.avi.archiver.file.FilenamePattern;
import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.populator.BulletinHeadingDataPopulator.BulletinHeadingSource;
import fi.fmi.avi.model.GenericAviationWeatherMessage;
import fi.fmi.avi.model.MessageType;
import fi.fmi.avi.model.PartialDateTime;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.bulletin.BulletinHeading;
import fi.fmi.avi.model.bulletin.DataTypeDesignatorT2;
import fi.fmi.avi.model.bulletin.immutable.BulletinHeadingImpl;

@SuppressWarnings("UnnecessaryLocalVariable")
class BulletinHeadingDataPopulatorTest {
    private static final Pattern FILE_NAME_PATTERN = Pattern.compile("(metar|taf|tca|speci|sigmet|vaa|airmet|swx)"
            + "(?:_(?:(?<yyyy>\\d{4})-)?(?:(?<MM>\\d{2})-)?(?<dd>\\d{2})?T(?<hh>\\d{2})?(?::(?<mm>\\d{2}))?(?::(?<ss>\\d{2}))?)?" + "(?:\\.txt|\\.xml)");
    private static final ArchiveAviationMessage EMPTY_RESULT = ArchiveAviationMessage.builder().buildPartial();
    private static final Map<GenericAviationWeatherMessage.Format, Integer> FORMAT_IDS = Arrays.stream(FormatId.values())//
            .collect(Maps.toImmutableEnumMap(FormatId::getFormat, FormatId::getId));
    private static final Map<MessageType, Integer> TYPE_IDS = Arrays.stream(TypeId.values())//
            .collect(ImmutableMap.toImmutableMap(TypeId::getType, TypeId::getId));
    private static final List<BulletinHeadingSource> BULLETIN_HEADING_SOURCES = Arrays.asList(BulletinHeadingSource.GTS_BULLETIN_HEADING,
            BulletinHeadingSource.COLLECT_IDENTIFIER);
    private static final InputAviationMessage INPUT_MESSAGE_TEMPLATE = InputAviationMessage.builder()//
            .setFileMetadata(FileMetadata.builder()//
                    .setProductIdentifier("testproduct")//
                    .setFileModified(Instant.parse("2000-01-02T03:05:34Z"))//
                    .setFilenamePattern(new FilenamePattern("taf.txt", FILE_NAME_PATTERN, ZoneOffset.UTC)))//
            .buildPartial();
    private static final BulletinHeadingImpl BULLETIN_HEADING_TEMPLATE = BulletinHeadingImpl.builder()//
            .setDataTypeDesignatorT2(DataTypeDesignatorT2.ForecastsDataTypeDesignatorT2.FCT_AERODROME_VT_LONG)//
            .setLocationIndicator("YUDO")//
            .setGeographicalDesignator("YY")//
            .setBulletinNumber(1)//
            .mutateIssueTime(issueTime -> issueTime.setCompleteTime(ZonedDateTime.parse("2000-01-02T03:00Z")))//
            .build();

    private BulletinHeadingDataPopulator populator;

    @BeforeEach
    void setUp() {
        populator = new BulletinHeadingDataPopulator(FORMAT_IDS, TYPE_IDS, BULLETIN_HEADING_SOURCES);
    }

    @Test
    void does_nothing_when_bulletin_information_is_not_available() {
        final InputAviationMessage inputMessage = INPUT_MESSAGE_TEMPLATE;
        final ArchiveAviationMessage expected = EMPTY_RESULT;

        final ArchiveAviationMessage.Builder builder = EMPTY_RESULT.toBuilder();
        populator.populate(inputMessage, builder);
        assertThat(builder.buildPartial()).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({//
            "FCT_AERODROME_VT_LONG, , TAC", //
            ", FCT_AERODROME_VT_LONG, TAC", //
            "FCT_AERODROME_VT_LONG, FCT_AERODROME_VT_LONG, TAC", //
            "FCT_AERODROME_VT_LONG, XML_AERODROME_VT_LONG, TAC", //
            "XML_AERODROME_VT_LONG, , IWXXM", //
            ", XML_AERODROME_VT_LONG, IWXXM", //
            "XML_AERODROME_VT_LONG, XML_AERODROME_VT_LONG, IWXXM", //
            "XML_AERODROME_VT_LONG, FCT_AERODROME_VT_LONG, IWXXM", //
    })
    void populates_format_when_exists(@Nullable final T2 gtsDesignatorT2, @Nullable final T2 collectDesignatorT2, final FormatId expectedFormat) {
        final InputAviationMessage.Builder inputMessageBuilder = INPUT_MESSAGE_TEMPLATE.toBuilder();
        if (gtsDesignatorT2 != null) {
            inputMessageBuilder.mutateGtsBulletinHeading(
                    builder -> builder.setBulletinHeading(BULLETIN_HEADING_TEMPLATE.toBuilder().setDataTypeDesignatorT2(gtsDesignatorT2.get()).build()));
        }
        if (collectDesignatorT2 != null) {
            inputMessageBuilder.mutateCollectIdentifier(
                    builder -> builder.setBulletinHeading(BULLETIN_HEADING_TEMPLATE.toBuilder().setDataTypeDesignatorT2(collectDesignatorT2.get()).build()));
        }
        final InputAviationMessage inputMessage = inputMessageBuilder.buildPartial();

        final ArchiveAviationMessage.Builder builder = EMPTY_RESULT.toBuilder();
        populator.populate(inputMessage, builder);
        assertThat(builder.getFormat()).isEqualTo(expectedFormat.getId());
    }

    @ParameterizedTest
    @CsvSource({//
            "XML_METAR, , METAR", //
            ", XML_METAR, METAR", //
            "XML_METAR, XML_METAR, METAR", //
            "XML_METAR, XML_SPACE_WEATHER_ADVISORY, METAR", //
            "XML_SPACE_WEATHER_ADVISORY, XML_METAR, SWX", //

            "XML_AERODROME_VT_SHORT, , TAF", //
            ", XML_AERODROME_VT_SHORT, TAF", //
            "XML_AERODROME_VT_SHORT, XML_AERODROME_VT_SHORT, TAF", //
            "XML_AERODROME_VT_SHORT, XML_SPACE_WEATHER_ADVISORY, TAF", //
            "XML_SPACE_WEATHER_ADVISORY, XML_AERODROME_VT_SHORT, SWX", //

            "WRN_AIRMET, , AIRMET", //
            ", WRN_AIRMET, AIRMET", //
            "WRN_AIRMET, WRN_AIRMET, AIRMET", //
            "WRN_AIRMET, FCT_SPACE_WEATHER, AIRMET", //
            "FCT_SPACE_WEATHER, WRN_AIRMET, SWX", //
    })
    void populates_type_when_exists(@Nullable final T2 gtsDesignatorT2, @Nullable final T2 collectDesignatorT2, final TypeId expectedType) {
        final InputAviationMessage.Builder inputMessageBuilder = INPUT_MESSAGE_TEMPLATE.toBuilder();
        if (gtsDesignatorT2 != null) {
            inputMessageBuilder.mutateGtsBulletinHeading(
                    builder -> builder.setBulletinHeading(BULLETIN_HEADING_TEMPLATE.toBuilder().setDataTypeDesignatorT2(gtsDesignatorT2.get()).build()));
        }
        if (collectDesignatorT2 != null) {
            inputMessageBuilder.mutateCollectIdentifier(
                    builder -> builder.setBulletinHeading(BULLETIN_HEADING_TEMPLATE.toBuilder().setDataTypeDesignatorT2(collectDesignatorT2.get()).build()));
        }
        final InputAviationMessage inputMessage = inputMessageBuilder.buildPartial();

        final ArchiveAviationMessage.Builder builder = EMPTY_RESULT.toBuilder();
        populator.populate(inputMessage, builder);
        assertThat(builder.getType()).isEqualTo(expectedType.getId());
    }

    @ParameterizedTest
    @CsvSource({//
            // Return native complete time when available
            ", 2000-01-02T03:04:01Z, taf_2000-01-02T03:05.txt, 2005-01-02T03:05:06Z, 2000-01-02T03:04:01Z", //
            // Complete partial time with filename time
            "--02T03:04Z, , taf_2000-01-02T03:05.txt, 2005-01-02T03:05:06Z, 2000-01-02T03:04:00Z", //
            // Complete partial time with filename time completed with file time when filename time is partial
            "--T03:04Z, , taf_02T03:05.txt, 2005-01-07T03:05:06Z, 2005-01-02T03:04:00Z", //
            // Complete partial time with file time when filename time is not available
            "--02T03:04Z, , taf.txt, 2000-01-02T03:05:06Z, 2000-01-02T03:04:00Z", //
            // Return resolved partial time from preferred source, even when secondary source offers native complete time:
            "--02T03:04Z, 2000-01-02T03:04:01Z, taf_2000-01-02T03:05.txt, 2005-01-02T03:05:06Z, 2000-01-02T03:04:00Z", //
    })
    void populates_messageTime_when_exists(@Nullable final PartialDateTime gtsIssueTime, @Nullable final ZonedDateTime collectIssueTime, final String filename,
            final Instant fileModified, final Instant expectedTime) {
        final InputAviationMessage.Builder inputMessageBuilder = INPUT_MESSAGE_TEMPLATE.toBuilder()//
                .mutateFileMetadata(filedata -> filedata.setFileModified(fileModified)//
                        .setFilenamePattern(new FilenamePattern(filename, FILE_NAME_PATTERN, ZoneOffset.UTC)));
        if (gtsIssueTime != null) {
            inputMessageBuilder.mutateGtsBulletinHeading(builder -> builder.setBulletinHeading(
                    BULLETIN_HEADING_TEMPLATE.toBuilder().setIssueTime(PartialOrCompleteTimeInstant.of(gtsIssueTime)).build()));
        }
        if (collectIssueTime != null) {
            inputMessageBuilder.mutateCollectIdentifier(builder -> builder.setBulletinHeading(
                    BULLETIN_HEADING_TEMPLATE.toBuilder().setIssueTime(PartialOrCompleteTimeInstant.of(collectIssueTime)).build()));
        }
        final InputAviationMessage inputMessage = inputMessageBuilder.buildPartial();

        final ArchiveAviationMessage.Builder builder = EMPTY_RESULT.toBuilder();
        populator.populate(inputMessage, builder);
        assertThat(builder.getMessageTime()).isEqualTo(expectedTime);
    }

    @ParameterizedTest
    @CsvSource({//
            "YUDO, , YUDO", //
            ", YUDO, YUDO", //
            "YUDO, XXXX, YUDO", //
    })
    void populates_icaoAirportCode_when_exists(@Nullable final String gtsLocationIndicator, @Nullable final String collectLocationIndicator,
            final String expectedIcaoAirportCode) {
        final InputAviationMessage.Builder inputMessageBuilder = INPUT_MESSAGE_TEMPLATE.toBuilder();
        if (gtsLocationIndicator != null) {
            inputMessageBuilder.mutateGtsBulletinHeading(
                    builder -> builder.setBulletinHeading(BULLETIN_HEADING_TEMPLATE.toBuilder().setLocationIndicator(gtsLocationIndicator).build()));
        }
        if (collectLocationIndicator != null) {
            inputMessageBuilder.mutateCollectIdentifier(
                    builder -> builder.setBulletinHeading(BULLETIN_HEADING_TEMPLATE.toBuilder().setLocationIndicator(collectLocationIndicator).build()));
        }
        final InputAviationMessage inputMessage = inputMessageBuilder.buildPartial();

        final ArchiveAviationMessage.Builder builder = EMPTY_RESULT.toBuilder();
        populator.populate(inputMessage, builder);
        assertThat(builder.getIcaoAirportCode()).isEqualTo(expectedIcaoAirportCode);
    }

    @ParameterizedTest
    @CsvSource({//
            ", , ", //
            "GTS HEADING, , GTS HEADING", //
            ", collect_identifier.xml, ", //
            "GTS HEADING, collect_identifier.xml, GTS HEADING", //
    })
    void populates_heading_when_exists(@Nullable final String gtsBulletinHeadingString, @Nullable final String collectIdentifierString,
            @Nullable final String expectedHeading) {
        final InputAviationMessage.Builder inputMessageBuilder = INPUT_MESSAGE_TEMPLATE.toBuilder();
        if (gtsBulletinHeadingString != null) {
            inputMessageBuilder.mutateGtsBulletinHeading(builder -> builder.setBulletinHeadingString(gtsBulletinHeadingString));
        }
        if (collectIdentifierString != null) {
            inputMessageBuilder.mutateCollectIdentifier(builder -> builder.setBulletinHeadingString(collectIdentifierString));
        }
        final InputAviationMessage inputMessage = inputMessageBuilder.buildPartial();

        final ArchiveAviationMessage.Builder builder = EMPTY_RESULT.toBuilder();
        populator.populate(inputMessage, builder);
        assertThat(builder.getHeading()).isEqualTo(Optional.ofNullable(expectedHeading));
    }

    @ParameterizedTest
    @CsvSource({//
            ", , , , ", //
            "NORMAL, , , , ", //
            ", , NORMAL, , ", //
            "NORMAL, , NORMAL, , ", //
            "DELAYED, 1, , , RRA", //
            ", , DELAYED, 1, RRA", //
            "DELAYED, 1, DELAYED, 2, RRA", //
            "DELAYED, 2, DELAYED, 1, RRB", //
            "DELAYED, 26, , , RRZ", //
            "AMENDED, 1, , , AAA", //
            ", , AMENDED, 1, AAA", //
            "AMENDED, 1, AMENDED, 2, AAA", //
            "AMENDED, 2, AMENDED, 1, AAB", //
            "AMENDED, 26, , , AAZ", //
            "CORRECTED, 1, , , CCA", //
            ", , CORRECTED, 1, CCA", //
            "CORRECTED, 1, CORRECTED, 2, CCA", //
            "CORRECTED, 2, CORRECTED, 1, CCB", //
            "CORRECTED, 26, , , CCZ", //
    })
    void populates_version_when_exists(@Nullable final BulletinHeading.Type gtsBulletinType, @Nullable final Integer gtsAugmentationNumber,
            @Nullable final BulletinHeading.Type collectBulletinType, @Nullable final Integer collectAugmentationNumber,
            @Nullable final String expectedVersion) {
        final InputAviationMessage.Builder inputMessageBuilder = INPUT_MESSAGE_TEMPLATE.toBuilder();
        if (gtsBulletinType != null) {
            inputMessageBuilder.mutateGtsBulletinHeading(builder -> builder.setBulletinHeading(BULLETIN_HEADING_TEMPLATE.toBuilder()//
                    .setType(gtsBulletinType)//
                    .setNullableBulletinAugmentationNumber(gtsAugmentationNumber)//
                    .build()));
        }
        if (collectBulletinType != null) {
            inputMessageBuilder.mutateCollectIdentifier(builder -> builder.setBulletinHeading(BULLETIN_HEADING_TEMPLATE.toBuilder()//
                    .setType(collectBulletinType)//
                    .setNullableBulletinAugmentationNumber(collectAugmentationNumber)//
                    .build()));
        }
        final InputAviationMessage inputMessage = inputMessageBuilder.buildPartial();

        final ArchiveAviationMessage.Builder builder = EMPTY_RESULT.toBuilder();
        populator.populate(inputMessage, builder);
        assertThat(builder.getVersion()).isEqualTo(Optional.ofNullable(expectedVersion));
    }

    @ParameterizedTest
    @CsvSource({//
            ", , ", //
            "GTS HEADING, , ", //
            ", collect_identifier.xml, collect_identifier.xml", //
            "GTS HEADING, collect_identifier.xml, collect_identifier.xml", //
    })
    void populates_collectIdentifier_when_exists(@Nullable final String gtsBulletinHeadingString, @Nullable final String collectIdentifierString,
            @Nullable final String expectedCollectIdentifier) {
        final InputAviationMessage.Builder inputMessageBuilder = INPUT_MESSAGE_TEMPLATE.toBuilder();
        if (gtsBulletinHeadingString != null) {
            inputMessageBuilder.mutateGtsBulletinHeading(builder -> builder.setBulletinHeadingString(gtsBulletinHeadingString));
        }
        if (collectIdentifierString != null) {
            inputMessageBuilder.mutateCollectIdentifier(builder -> builder.setBulletinHeadingString(collectIdentifierString));
        }
        final InputAviationMessage inputMessage = inputMessageBuilder.buildPartial();

        final ArchiveAviationMessage.Builder builder = EMPTY_RESULT.toBuilder();
        populator.populate(inputMessage, builder);
        assertThat(builder.getIWXXMDetailsBuilder().getCollectIdentifier()).isEqualTo(Optional.ofNullable(expectedCollectIdentifier));
    }

    enum FormatId {
        TAC(GenericAviationWeatherMessage.Format.TAC, 1), //
        IWXXM(GenericAviationWeatherMessage.Format.IWXXM, 2), //
        ;

        private final GenericAviationWeatherMessage.Format format;
        private final int id;

        FormatId(final GenericAviationWeatherMessage.Format format, final int id) {
            this.format = format;
            this.id = id;
        }

        public static FormatId valueOf(final GenericAviationWeatherMessage.Format format) {
            return Arrays.stream(values())//
                    .filter(formatId -> formatId.getFormat() == format)//
                    .findAny()//
                    .orElseThrow(() -> new IllegalArgumentException("Unknown format: " + format));
        }

        public GenericAviationWeatherMessage.Format getFormat() {
            return format;
        }

        public int getId() {
            return id;
        }
    }

    enum TypeId {
        SPECI(MessageType.SPECI, 1), //
        METAR(MessageType.METAR, 2), //
        TAF(MessageType.TAF, 3), //
        SIGMET(MessageType.SIGMET, 4), //
        AIRMET(MessageType.AIRMET, 5), //
        TCA(MessageType.TROPICAL_CYCLONE_ADVISORY, 6), //
        VAA(MessageType.VOLCANIC_ASH_ADVISORY, 7), //
        SWX(MessageType.SPACE_WEATHER_ADVISORY, 8), //
        ;

        private final MessageType type;
        private final int id;

        TypeId(final MessageType type, final int id) {
            this.type = type;
            this.id = id;
        }

        public MessageType getType() {
            return type;
        }

        public int getId() {
            return id;
        }
    }

    enum T2 {
        XML_METAR(DataTypeDesignatorT2.XMLDataTypeDesignatorT2.XML_METAR), //
        XML_AERODROME_VT_SHORT(DataTypeDesignatorT2.XMLDataTypeDesignatorT2.XML_AERODROME_VT_SHORT), //
        XML_TROPICAL_CYCLONE_ADVISORIES(DataTypeDesignatorT2.XMLDataTypeDesignatorT2.XML_TROPICAL_CYCLONE_ADVISORIES), //
        XML_SPECI(DataTypeDesignatorT2.XMLDataTypeDesignatorT2.XML_SPECI), //
        XML_SIGMET(DataTypeDesignatorT2.XMLDataTypeDesignatorT2.XML_SIGMET), //
        XML_AERODROME_VT_LONG(DataTypeDesignatorT2.XMLDataTypeDesignatorT2.XML_AERODROME_VT_LONG), //
        XML_VOLCANIC_ASH_ADVISORY(DataTypeDesignatorT2.XMLDataTypeDesignatorT2.XML_VOLCANIC_ASH_ADVISORY), //
        XML_VOLCANIC_ASH_SIGMET(DataTypeDesignatorT2.XMLDataTypeDesignatorT2.XML_VOLCANIC_ASH_SIGMET), //
        XML_AIRMET(DataTypeDesignatorT2.XMLDataTypeDesignatorT2.XML_AIRMET), //
        XML_TROPICAL_CYCLONE_SIGMET(DataTypeDesignatorT2.XMLDataTypeDesignatorT2.XML_TROPICAL_CYCLONE_SIGMET), //
        XML_SPACE_WEATHER_ADVISORY(DataTypeDesignatorT2.XMLDataTypeDesignatorT2.XML_SPACE_WEATHER_ADVISORY), //

        WRN_AIRMET(DataTypeDesignatorT2.WarningsDataTypeDesignatorT2.WRN_AIRMET), //
        WRN_TROPICAL_CYCLONE_SIGMET(DataTypeDesignatorT2.WarningsDataTypeDesignatorT2.WRN_TROPICAL_CYCLONE_SIGMET), //
        WRN_SIGMET(DataTypeDesignatorT2.WarningsDataTypeDesignatorT2.WRN_SIGMET), //
        WRN_VOLCANIC_ASH_CLOUDS_SIGMET(DataTypeDesignatorT2.WarningsDataTypeDesignatorT2.WRN_VOLCANIC_ASH_CLOUDS_SIGMET), //

        FCT_AERODROME_VT_SHORT(DataTypeDesignatorT2.ForecastsDataTypeDesignatorT2.FCT_AERODROME_VT_SHORT), //
        FCT_TROPICAL_CYCLONE_ADVISORIES(DataTypeDesignatorT2.ForecastsDataTypeDesignatorT2.FCT_TROPICAL_CYCLONE_ADVISORIES), //
        FCT_SPACE_WEATHER(DataTypeDesignatorT2.ForecastsDataTypeDesignatorT2.FCT_SPACE_WEATHER), //
        FCT_AERODROME_VT_LONG(DataTypeDesignatorT2.ForecastsDataTypeDesignatorT2.FCT_AERODROME_VT_LONG), //
        FCT_VOLCANIC_ASH_ADVISORIES(DataTypeDesignatorT2.ForecastsDataTypeDesignatorT2.FCT_VOLCANIC_ASH_ADVISORIES), //
        ;

        private final DataTypeDesignatorT2 value;

        T2(final DataTypeDesignatorT2 value) {
            this.value = value;
        }

        public DataTypeDesignatorT2 get() {
            return value;
        }
    }
}
