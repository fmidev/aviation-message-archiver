package fi.fmi.avi.archiver.message;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.google.common.collect.ImmutableMap;

import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.AviMessageSpecificConverter;
import fi.fmi.avi.converter.tac.conf.TACConverter;
import fi.fmi.avi.model.GenericMeteorologicalBulletin;
import fi.fmi.avi.model.MessageType;

public class MessageParserTest {

    private MessageParser messageParser;
    private Clock clock;

    static AviMessageConverter getAviMessageConverter() {
        return AviMessageConverterHolder.INSTANCE;
    }

    @Before
    public void setUp() {
        clock = Clock.fixed(Instant.parse("2019-05-05T10:21:20Z"), ZoneId.of("UTC"));
        messageParser = new MessageParser(clock, getAviMessageConverter(), new ImmutableMap.Builder<MessageType, Integer>()//
                .put(MessageType.TAF, 1)//
                .build());
    }

    @Test
    public void parse_single_taf() {
        final AviationMessageFilenamePattern filenamePattern = new AviationMessageFilenamePattern("TAF_20190505_102013_12332319",
                Pattern.compile("^TAF_(?<yyyy>\\d{4})(?<MM>\\d{2})(?<dd>\\d{2})_(?<hh>\\d{2})(?<mm>\\d{2})(?<ss>\\d{2})_\\d{8}$"));
        final String content = "FTFI33 EFPP 020500\n"
                + "TAF EFKE 020532Z 0206/0312 05005KT 9999 -SHRA BKN004 BECMG 0206/0208 FEW005 BKN020 TEMPO 0206/0215 4000 SHRA BKN010 SCT030CB=";
        final Instant currentTime = clock.instant();

        final List<AviationMessage> parsedMessages = messageParser.parse(1, filenamePattern, content, currentTime, currentTime.minusSeconds(60));
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
        assertThat(taf.getFileModified()).hasValue(Instant.parse("2019-05-05T10:20:20Z"));
        assertThat(taf.getVersion()).isEmpty();
    }

    @Test
    public void parse_single_cnl_taf() {
        final AviationMessageFilenamePattern filenamePattern = new AviationMessageFilenamePattern("TAF_20190505_102013_12332319",
                Pattern.compile("^TAF_(?<yyyy>\\d{4})(?<MM>\\d{2})(?<dd>\\d{2})_(?<hh>\\d{2})(?<mm>\\d{2})(?<ss>\\d{2})_\\d{8}$"));
        final String content = "FTXX33 YYYY 020500 AAA\n" + "TAF AMD YYYY 020532Z 0206/0312 CNL=";
        final Instant currentTime = clock.instant();

        final List<AviationMessage> parsedMessages = messageParser.parse(1, filenamePattern, content, currentTime, currentTime.minusSeconds(60));
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
        assertThat(taf.getFileModified()).hasValue(Instant.parse("2019-05-05T10:20:20Z"));
        assertThat(taf.getVersion()).isEqualTo(Optional.of("AAA"));
    }

    @Test
    public void parse_single_corr_taf() {
        final AviationMessageFilenamePattern filenamePattern = new AviationMessageFilenamePattern("TAF_20190505_102013_12332319",
                Pattern.compile("^TAF_(?<yyyy>\\d{4})(?<MM>\\d{2})(?<dd>\\d{2})_(?<hh>\\d{2})(?<mm>\\d{2})(?<ss>\\d{2})_\\d{8}$"));
        final String content = "FTXX33 YYYY 020500 CCA\n" + "TAF COR YYYY 020532Z 0206/0312 20108KT 8000=";
        final Instant currentTime = clock.instant();

        final List<AviationMessage> parsedMessages = messageParser.parse(1, filenamePattern, content, currentTime, currentTime.minusSeconds(60));
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
        assertThat(taf.getFileModified()).hasValue(Instant.parse("2019-05-05T10:20:20Z"));
        assertThat(taf.getVersion()).isEqualTo(Optional.of("CCA"));
    }

    private static final class AviMessageConverterHolder {
        private static final AviMessageConverter INSTANCE = createInstance();

        private static AviMessageConverter createInstance() {
            final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ConverterContextConfiguration.class);
            final AviMessageConverter converter = context.getBean(AviMessageConverter.class);
            context.close();
            return converter;
        }
    }

    @Configuration
    @Import({ TACConverter.class })
    static class ConverterContextConfiguration {
        @Autowired
        private AviMessageSpecificConverter<String, GenericMeteorologicalBulletin> genericBulletinTACParser;

        @Bean
        public AviMessageConverter aviMessageConverter() {
            final AviMessageConverter aviMessageConverter = new AviMessageConverter();
            aviMessageConverter.setMessageSpecificConverter(TACConverter.TAC_TO_GENERIC_BULLETIN_POJO, genericBulletinTACParser);
            return aviMessageConverter;
        }
    }

}
