package fi.fmi.avi.archiver.message.populator;

import com.google.common.collect.ImmutableMap;
import fi.fmi.avi.archiver.file.FileConfig;
import fi.fmi.avi.archiver.file.FileMetadata;
import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.file.InputBulletinHeading;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.model.GenericAviationWeatherMessage;
import fi.fmi.avi.model.MessageType;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.PartialOrCompleteTimePeriod;
import fi.fmi.avi.model.immutable.GenericAviationWeatherMessageImpl;
import fi.fmi.avi.util.BulletinHeadingDecoder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

public class BaseDataPopulatorTest {

    private BaseDataPopulator baseDataPopulator;
    private Clock clock;

    @BeforeEach
    public void setUp() {
        clock = Clock.fixed(Instant.parse("2019-05-05T10:21:20Z"), ZoneId.of("UTC"));
        baseDataPopulator = new BaseDataPopulator(clock, new ImmutableMap.Builder<MessageType, Integer>()//
                .put(MessageType.TAF, 1)//
                .build());
    }

    @Test
    public void parse_single_taf() {
        final GenericAviationWeatherMessage message = GenericAviationWeatherMessageImpl.builder()
                .setOriginalMessage("TAF EFKE 020532Z 0206/0312 05005KT 9999 -SHRA BKN004 BECMG 0206/0208 FEW005 BKN020 TEMPO 0206/0215 4000 SHRA BKN010 SCT030CB=")
                .setIssueTime(PartialOrCompleteTimeInstant.createIssueTime("020532Z"))
                .setValidityTime(PartialOrCompleteTimePeriod.createValidityTime("0206/0312"))
                .putLocationIndicators(GenericAviationWeatherMessage.LocationIndicatorType.AERODROME, "EFKE")
                .setMessageType(MessageType.TAF)
                .setMessageFormat(GenericAviationWeatherMessage.Format.TAC)
                .setTranslated(false)
                .build();
        final InputBulletinHeading gtsBulletinHeading = InputBulletinHeading.builder()//
                .setBulletinHeadingString("FTFI33 EFPP 020500")
                .setBulletinHeading(BulletinHeadingDecoder.decode("FTFI33 EFPP 020500", null))
                .build();
        final FileMetadata fileMetadata = FileMetadata.builder()
                .setFilename("TAF_20190505_102013_12332319")
                .setFileConfig(new FileConfig.Builder()//
                        .setPattern(Pattern.compile("^TAF_(?<yyyy>\\d{4})(?<MM>\\d{2})(?<dd>\\d{2})_(?<hh>\\d{2})(?<mm>\\d{2})(?<ss>\\d{2})_\\d{8}$"))
                        .setNameTimeZone(ZoneId.of("Z"))
                        .setFormat(GenericAviationWeatherMessage.Format.TAC)
                        .setFormatId(0)
                        .build())
                .setFileModified(clock.instant())
                .setProductIdentifier("test identifier")
                .build();
        final InputAviationMessage inputAviationMessage = InputAviationMessage.builder()
                .setGtsBulletinHeading(gtsBulletinHeading)
                .setFileMetadata(fileMetadata)
                .setMessage(message)
                .build();

        final ArchiveAviationMessage.Builder builder = ArchiveAviationMessage.builder();
        baseDataPopulator.populate(inputAviationMessage, builder);
        final ArchiveAviationMessage taf = builder.build();

        assertThat(taf.getType()).isEqualTo(1);
        assertThat(taf.getRoute()).isEqualTo(1);
        assertThat(taf.getMessageTime()).isEqualTo(Instant.parse("2019-05-02T05:32:00Z"));
        assertThat(taf.getIcaoAirportCode()).isEqualTo("EFKE");
        assertThat(taf.getHeading()).contains("FTFI33 EFPP 020500");
        assertThat(taf.getMessage()).isEqualTo(
                "TAF EFKE 020532Z 0206/0312 05005KT 9999 -SHRA BKN004 BECMG 0206/0208 FEW005 BKN020 TEMPO 0206/0215 4000 SHRA " + "BKN010 SCT030CB=");
        assertThat(taf.getValidFrom()).hasValue(Instant.parse("2019-05-02T06:00:00Z"));
        assertThat(taf.getValidTo()).hasValue(Instant.parse("2019-05-03T12:00:00Z"));
        assertThat(taf.getFileModified()).hasValue(Instant.parse("2019-05-05T10:21:20Z"));
        assertThat(taf.getVersion()).isEmpty();
    }

    @Test
    public void parse_single_cnl_taf() {
        final GenericAviationWeatherMessage message = GenericAviationWeatherMessageImpl.builder()
                .setOriginalMessage("TAF AMD YYYY 020532Z 0206/0312 CNL=")
                .setIssueTime(PartialOrCompleteTimeInstant.createIssueTime("020532Z"))
                .setValidityTime(PartialOrCompleteTimePeriod.createValidityTime("0206/0312"))
                .putLocationIndicators(GenericAviationWeatherMessage.LocationIndicatorType.AERODROME, "YYYY")
                .setMessageType(MessageType.TAF)
                .setMessageFormat(GenericAviationWeatherMessage.Format.TAC)
                .setTranslated(false)
                .build();
        final InputBulletinHeading gtsBulletinHeading = InputBulletinHeading.builder()//
                .setBulletinHeadingString("FTXX33 YYYY 020500 AAA")
                .setBulletinHeading(BulletinHeadingDecoder.decode("FTXX33 YYYY 020500 AAA", null))
                .build();
        final FileMetadata fileMetadata = FileMetadata.builder()
                .setFilename("TAF_20190505_102013_12332319")
                .setFileConfig(new FileConfig.Builder()//
                        .setPattern(Pattern.compile("^TAF_(?<yyyy>\\d{4})(?<MM>\\d{2})(?<dd>\\d{2})_(?<hh>\\d{2})(?<mm>\\d{2})(?<ss>\\d{2})_\\d{8}$"))
                        .setNameTimeZone(ZoneId.of("Z"))
                        .setFormat(GenericAviationWeatherMessage.Format.TAC)
                        .setFormatId(0)
                        .build())
                .setFileModified(clock.instant())
                .setProductIdentifier("test identifier")
                .build();
        final InputAviationMessage inputAviationMessage = InputAviationMessage.builder()
                .setGtsBulletinHeading(gtsBulletinHeading)
                .setFileMetadata(fileMetadata)
                .setMessage(message)
                .build();

        final ArchiveAviationMessage.Builder builder = ArchiveAviationMessage.builder();
        baseDataPopulator.populate(inputAviationMessage, builder);
        final ArchiveAviationMessage taf = builder.build();

        assertThat(taf.getType()).isEqualTo(1);
        assertThat(taf.getRoute()).isEqualTo(1);
        assertThat(taf.getMessageTime()).isEqualTo(Instant.parse("2019-05-02T05:32:00Z"));
        assertThat(taf.getIcaoAirportCode()).isEqualTo("YYYY");
        assertThat(taf.getHeading()).contains("FTXX33 YYYY 020500 AAA");
        assertThat(taf.getMessage()).isEqualTo("TAF AMD YYYY 020532Z 0206/0312 CNL=");
        assertThat(taf.getValidFrom()).hasValue(Instant.parse("2019-05-02T06:00:00Z"));
        assertThat(taf.getValidTo()).hasValue(Instant.parse("2019-05-03T12:00:00Z"));
        assertThat(taf.getFileModified()).hasValue(Instant.parse("2019-05-05T10:21:20Z"));
        assertThat(taf.getVersion()).isEqualTo(Optional.of("AAA"));
    }

    @Test
    public void parse_single_corr_taf() {
        final GenericAviationWeatherMessage message = GenericAviationWeatherMessageImpl.builder()
                .setOriginalMessage("TAF COR YYYY 020532Z 0206/0312 20108KT 8000=")
                .setIssueTime(PartialOrCompleteTimeInstant.createIssueTime("020532Z"))
                .setValidityTime(PartialOrCompleteTimePeriod.createValidityTime("0206/0312"))
                .putLocationIndicators(GenericAviationWeatherMessage.LocationIndicatorType.AERODROME, "YYYY")
                .setMessageType(MessageType.TAF)
                .setMessageFormat(GenericAviationWeatherMessage.Format.TAC)
                .setTranslated(false)
                .build();
        final InputBulletinHeading gtsBulletinHeading = InputBulletinHeading.builder()//
                .setBulletinHeadingString("FTXX33 YYYY 020500 CCA")
                .setBulletinHeading(BulletinHeadingDecoder.decode("FTXX33 YYYY 020500 CCA", null))
                .build();
        final FileMetadata fileMetadata = FileMetadata.builder()
                .setFilename("TAF_20190505_102013_12332319")
                .setFileConfig(new FileConfig.Builder()//
                        .setPattern(Pattern.compile("^TAF_(?<yyyy>\\d{4})(?<MM>\\d{2})(?<dd>\\d{2})_(?<hh>\\d{2})(?<mm>\\d{2})(?<ss>\\d{2})_\\d{8}$"))
                        .setNameTimeZone(ZoneId.of("Z"))
                        .setFormat(GenericAviationWeatherMessage.Format.TAC)
                        .setFormatId(0)
                        .build())
                .setFileModified(clock.instant())
                .setProductIdentifier("test identifier")
                .build();
        final InputAviationMessage inputAviationMessage = InputAviationMessage.builder()
                .setGtsBulletinHeading(gtsBulletinHeading)
                .setFileMetadata(fileMetadata)
                .setMessage(message)
                .build();

        final ArchiveAviationMessage.Builder builder = ArchiveAviationMessage.builder();
        baseDataPopulator.populate(inputAviationMessage, builder);
        final ArchiveAviationMessage taf = builder.build();

        assertThat(taf.getType()).isEqualTo(1);
        assertThat(taf.getRoute()).isEqualTo(1);
        assertThat(taf.getMessageTime()).isEqualTo(Instant.parse("2019-05-02T05:32:00Z"));
        assertThat(taf.getIcaoAirportCode()).isEqualTo("YYYY");
        assertThat(taf.getHeading()).contains("FTXX33 YYYY 020500 CCA");
        assertThat(taf.getMessage()).isEqualTo("TAF COR YYYY 020532Z 0206/0312 20108KT 8000=");
        assertThat(taf.getValidFrom()).hasValue(Instant.parse("2019-05-02T06:00:00Z"));
        assertThat(taf.getValidTo()).hasValue(Instant.parse("2019-05-03T12:00:00Z"));
        assertThat(taf.getFileModified()).hasValue(Instant.parse("2019-05-05T10:21:20Z"));
        assertThat(taf.getVersion()).isEqualTo(Optional.of("CCA"));
    }

}
