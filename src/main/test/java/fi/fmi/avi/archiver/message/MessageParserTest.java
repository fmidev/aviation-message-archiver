package fi.fmi.avi.archiver.message;

import com.google.common.collect.ImmutableMap;
import fi.fmi.avi.archiver.file.FileAviationMessage;
import fi.fmi.avi.archiver.file.FilenamePattern;
import fi.fmi.avi.model.MessageType;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.PartialOrCompleteTimePeriod;
import fi.fmi.avi.model.immutable.AerodromeImpl;
import fi.fmi.avi.util.BulletinHeadingDecoder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

public class MessageParserTest {

    private MessageParser messageParser;
    private Clock clock;

    @BeforeEach
    public void setUp() {
        clock = Clock.fixed(Instant.parse("2019-05-05T10:21:20Z"), ZoneId.of("UTC"));
        messageParser = new MessageParser(clock, new ImmutableMap.Builder<MessageType, Integer>()//
                .put(MessageType.TAF, 1)//
                .build());
    }

    @Test
    public void parse_single_taf() {
        final FileAviationMessage fileAviationMessage = FileAviationMessage.builder()
                .setFilenamePattern(new FilenamePattern("TAF_20190505_102013_12332319",
                        Pattern.compile("^TAF_(?<yyyy>\\d{4})(?<MM>\\d{2})(?<dd>\\d{2})_(?<hh>\\d{2})(?<mm>\\d{2})(?<ss>\\d{2})_\\d{8}$")))
                .setContent("TAF EFKE 020532Z 0206/0312 05005KT 9999 -SHRA BKN004 BECMG 0206/0208 FEW005 BKN020 TEMPO 0206/0215 4000 SHRA BKN010 SCT030CB=")
                .setGtsBulletinHeadingString("FTFI33 EFPP 020500")
                .setGtsBulletinHeading(BulletinHeadingDecoder.decode("FTFI33 EFPP 020500", null))
                .setFileModified(clock.instant())
                .setMessageTime(PartialOrCompleteTimeInstant.createIssueTime("020532Z"))
                .setValidityPeriod(PartialOrCompleteTimePeriod.createValidityTime("0206/0312"))
                .setLocationIndicator(AerodromeImpl.builder().setDesignator("EFKE").build())
                .setProductIdentifier("test identifier")
                .setType(MessageType.TAF)
                .build();

        final List<AviationMessage> parsedMessages = messageParser.parse(Collections.singletonList(fileAviationMessage));
        assertThat(parsedMessages).hasSize(1);

        final AviationMessage taf = parsedMessages.iterator().next();
        assertThat(taf.getType()).isEqualTo(1);
        assertThat(taf.getRoute()).isEqualTo(1);
        assertThat(taf.getMessageTime()).isEqualTo(Instant.parse("2019-05-02T05:32:00Z"));
        assertThat(taf.getIcaoAirportCode()).isEqualTo("EFKE");
        assertThat(taf.getHeading()).isEqualTo("FTFI33 EFPP 020500");
        assertThat(taf.getMessage()).isEqualTo(
                "TAF EFKE 020532Z 0206/0312 05005KT 9999 -SHRA BKN004 BECMG 0206/0208 FEW005 BKN020 TEMPO 0206/0215 4000 SHRA " + "BKN010 SCT030CB=");
        assertThat(taf.getValidFrom()).hasValue(Instant.parse("2019-05-02T06:00:00Z"));
        assertThat(taf.getValidTo()).hasValue(Instant.parse("2019-05-03T12:00:00Z"));
        assertThat(taf.getFileModified()).hasValue(Instant.parse("2019-05-05T10:21:20Z"));
        assertThat(taf.getVersion()).isEmpty();
    }

    @Test
    public void parse_single_cnl_taf() {
        final FileAviationMessage fileAviationMessage = FileAviationMessage.builder()
                .setFilenamePattern(new FilenamePattern("TAF_20190505_102013_12332319",
                        Pattern.compile("^TAF_(?<yyyy>\\d{4})(?<MM>\\d{2})(?<dd>\\d{2})_(?<hh>\\d{2})(?<mm>\\d{2})(?<ss>\\d{2})_\\d{8}$")))
                .setContent("TAF AMD YYYY 020532Z 0206/0312 CNL=")
                .setGtsBulletinHeadingString("FTXX33 YYYY 020500 AAA")
                .setGtsBulletinHeading(BulletinHeadingDecoder.decode("FTXX33 YYYY 020500 AAA", null))
                .setFileModified(clock.instant())
                .setMessageTime(PartialOrCompleteTimeInstant.createIssueTime("020532Z"))
                .setValidityPeriod(PartialOrCompleteTimePeriod.createValidityTime("0206/0312"))
                .setLocationIndicator(AerodromeImpl.builder().setDesignator("YYYY").build())
                .setProductIdentifier("test identifier")
                .setType(MessageType.TAF)
                .build();

        final List<AviationMessage> parsedMessages = messageParser.parse(Collections.singletonList(fileAviationMessage));
        assertThat(parsedMessages).hasSize(1);

        final AviationMessage taf = parsedMessages.iterator().next();
        assertThat(taf.getType()).isEqualTo(1);
        assertThat(taf.getRoute()).isEqualTo(1);
        assertThat(taf.getMessageTime()).isEqualTo(Instant.parse("2019-05-02T05:32:00Z"));
        assertThat(taf.getIcaoAirportCode()).isEqualTo("YYYY");
        assertThat(taf.getHeading()).isEqualTo("FTXX33 YYYY 020500 AAA");
        assertThat(taf.getMessage()).isEqualTo("TAF AMD YYYY 020532Z 0206/0312 CNL=");
        assertThat(taf.getValidFrom()).hasValue(Instant.parse("2019-05-02T06:00:00Z"));
        assertThat(taf.getValidTo()).hasValue(Instant.parse("2019-05-03T12:00:00Z"));
        assertThat(taf.getFileModified()).hasValue(Instant.parse("2019-05-05T10:21:20Z"));
        assertThat(taf.getVersion()).isEqualTo(Optional.of("AAA"));
    }

    @Test
    public void parse_single_corr_taf() {
        final FileAviationMessage fileAviationMessage = FileAviationMessage.builder()
                .setFilenamePattern(new FilenamePattern("TAF_20190505_102013_12332319",
                        Pattern.compile("^TAF_(?<yyyy>\\d{4})(?<MM>\\d{2})(?<dd>\\d{2})_(?<hh>\\d{2})(?<mm>\\d{2})(?<ss>\\d{2})_\\d{8}$")))
                .setContent("TAF COR YYYY 020532Z 0206/0312 20108KT 8000=")
                .setGtsBulletinHeadingString("FTXX33 YYYY 020500 CCA")
                .setGtsBulletinHeading(BulletinHeadingDecoder.decode("FTXX33 YYYY 020500 CCA", null))
                .setFileModified(clock.instant())
                .setMessageTime(PartialOrCompleteTimeInstant.createIssueTime("020532Z"))
                .setValidityPeriod(PartialOrCompleteTimePeriod.createValidityTime("0206/0312"))
                .setLocationIndicator(AerodromeImpl.builder().setDesignator("YYYY").build())
                .setProductIdentifier("test identifier")
                .setType(MessageType.TAF)
                .build();

        final List<AviationMessage> parsedMessages = messageParser.parse(Collections.singletonList(fileAviationMessage));
        assertThat(parsedMessages).hasSize(1);

        final AviationMessage taf = parsedMessages.iterator().next();
        assertThat(taf.getType()).isEqualTo(1);
        assertThat(taf.getRoute()).isEqualTo(1);
        assertThat(taf.getMessageTime()).isEqualTo(Instant.parse("2019-05-02T05:32:00Z"));
        assertThat(taf.getIcaoAirportCode()).isEqualTo("YYYY");
        assertThat(taf.getHeading()).isEqualTo("FTXX33 YYYY 020500 CCA");
        assertThat(taf.getMessage()).isEqualTo("TAF COR YYYY 020532Z 0206/0312 20108KT 8000=");
        assertThat(taf.getValidFrom()).hasValue(Instant.parse("2019-05-02T06:00:00Z"));
        assertThat(taf.getValidTo()).hasValue(Instant.parse("2019-05-03T12:00:00Z"));
        assertThat(taf.getFileModified()).hasValue(Instant.parse("2019-05-05T10:21:20Z"));
        assertThat(taf.getVersion()).isEqualTo(Optional.of("CCA"));
    }

}
