package fi.fmi.avi.archiver.message.file;

import static fi.fmi.avi.model.GenericAviationWeatherMessage.Format.IWXXM;
import static fi.fmi.avi.model.GenericAviationWeatherMessage.Format.TAC;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import fi.fmi.avi.archiver.config.ConverterConfig;
import fi.fmi.avi.archiver.file.FileParser;
import fi.fmi.avi.archiver.file.FilenamePattern;
import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.model.MessageType;

@SpringJUnitConfig(ConverterConfig.class)
public class FileParserTest {

    private static final String DEFAULT_FILENAME = "test_file";
    private static final FilenamePattern DEFAULT_FILENAME_PATTERN = new FilenamePattern(DEFAULT_FILENAME, Pattern.compile(""), ZoneOffset.UTC);
    private static final Instant FILE_MODIFIED = Instant.now();
    private static final String PRODUCT_IDENTIFIER = "test";

    @Autowired
    private AviMessageConverter aviMessageConverter;

    private FileParser fileParser;

    private static String getFileContent(final String filename) {
        try {
            final URL resource = requireNonNull(FileParserTest.class.getResource(filename));
            final Path path = Paths.get(resource.toURI());
            assertThat(path).exists();
            return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        } catch (final RuntimeException | URISyntaxException | IOException e) {
            fail(e);
            return null;
        }
    }

    @BeforeEach
    public void setUp() {
        fileParser = new FileParser(aviMessageConverter);
    }

    @Test
    public void empty_content() {
        assertThrows(IllegalStateException.class,
                () -> fileParser.parse("", DEFAULT_FILENAME, DEFAULT_FILENAME_PATTERN, FILE_MODIFIED, PRODUCT_IDENTIFIER, TAC));
    }

    @Test
    public void whitespace_content() {
        assertThrows(IllegalStateException.class,
                () -> fileParser.parse("\r\r\n ", DEFAULT_FILENAME, DEFAULT_FILENAME_PATTERN, FILE_MODIFIED, PRODUCT_IDENTIFIER, TAC));
    }

    @Test
    public void inconvertible_content() {
        final String filename = "inconvertible.txt";
        final FileParser.FileParseResult result = fileParser.parse(getFileContent(filename), filename, DEFAULT_FILENAME_PATTERN, FILE_MODIFIED,
                PRODUCT_IDENTIFIER, TAC);

        assertThat(result.getParseErrors()).isFalse();
        assertThat(result.getInputAviationMessages()).hasSize(1);

        final InputAviationMessage message = result.getInputAviationMessages().get(0);
        assertThat(message.getGtsBulletinHeading().getBulletinHeading()).isEmpty();
        assertThat(message.getGtsBulletinHeading().getBulletinHeadingString()).isEmpty();
        assertThat(message.getMessage().getOriginalMessage()).isEqualTo("Inconvertible message");
        assertThat(message.getCollectIdentifier().getBulletinHeading()).isEmpty();
        assertThat(message.getCollectIdentifier().getBulletinHeadingString()).isEmpty();
    }

    @Test
    public void taf_tac() {
        final String filename = "simple_taf.txt2";
        final FileParser.FileParseResult result = fileParser.parse(getFileContent(filename), filename, DEFAULT_FILENAME_PATTERN, FILE_MODIFIED,
                PRODUCT_IDENTIFIER, TAC);

        assertThat(result.getParseErrors()).isFalse();
        assertThat(result.getInputAviationMessages()).hasSize(1);

        final InputAviationMessage message = result.getInputAviationMessages().get(0);
        assertThat(message.getGtsBulletinHeading().getBulletinHeading()).isPresent();
        assertThat(message.getGtsBulletinHeading().getBulletinHeadingString()).contains("FTXX33 XXXX 181500");
        assertThat(message.getMessage().getOriginalMessage()).isEqualTo("TAF EFXX 181500Z 1812/1912 00000KT CAVOK=");
        assertThat(message.getCollectIdentifier().getBulletinHeading()).isEmpty();
        assertThat(message.getCollectIdentifier().getBulletinHeadingString()).isEmpty();
    }

    @Test
    public void taf_tac_without_gts_heading() {
        final String filename = "taf-missing-gts-heading.txt";
        final FileParser.FileParseResult result = fileParser.parse(getFileContent(filename), filename, DEFAULT_FILENAME_PATTERN, FILE_MODIFIED,
                PRODUCT_IDENTIFIER, TAC);

        assertThat(result.getParseErrors()).isFalse();
        assertThat(result.getInputAviationMessages()).hasSize(1);

        final InputAviationMessage message = result.getInputAviationMessages().get(0);
        assertThat(message.getGtsBulletinHeading().getBulletinHeading()).isEmpty();
        assertThat(message.getGtsBulletinHeading().getBulletinHeadingString()).isEmpty();
        assertThat(message.getMessage().getOriginalMessage()).isEqualTo("TAF EFXX 181500Z 1812/1912 00000KT CAVOK=");
        assertThat(message.getCollectIdentifier().getBulletinHeading()).isEmpty();
        assertThat(message.getCollectIdentifier().getBulletinHeadingString()).isEmpty();
    }

    @Test
    public void taf_tac_bulletin() {
        final String filename = "taf-tac-bulletin.bul";
        final FileParser.FileParseResult result = fileParser.parse(getFileContent(filename), filename, DEFAULT_FILENAME_PATTERN, FILE_MODIFIED,
                PRODUCT_IDENTIFIER, TAC);

        assertThat(result.getParseErrors()).isFalse();
        assertThat(result.getInputAviationMessages()).hasSize(2);

        assertThat(result.getInputAviationMessages()).allSatisfy(message -> {
            assertThat(message.getGtsBulletinHeading().getBulletinHeading()).isPresent();
            assertThat(message.getGtsBulletinHeading().getBulletinHeadingString()).contains("FTYU31 YUDO 160000");
            assertThat(message.getCollectIdentifier().getBulletinHeading()).isEmpty();
            assertThat(message.getCollectIdentifier().getBulletinHeadingString()).isEmpty();
        });

        assertThat(result.getInputAviationMessages().get(0).getMessage().getOriginalMessage()).isEqualTo("TAF YUDO 160000Z NIL=");
        assertThat(result.getInputAviationMessages().get(1).getMessage().getOriginalMessage()).isEqualTo("TAF YUDD 160000Z NIL=");
    }

    @Test
    public void taf_tac_bulletin_partially_valid() {
        final String filename = "taf-tac-bulletin-partially-valid.bul";
        final FileParser.FileParseResult result = fileParser.parse(getFileContent(filename), filename, DEFAULT_FILENAME_PATTERN, FILE_MODIFIED,
                PRODUCT_IDENTIFIER, TAC);

        assertThat(result.getParseErrors()).isTrue();
        assertThat(result.getInputAviationMessages()).hasSize(2);

        assertThat(result.getInputAviationMessages()).allSatisfy(message -> {
            assertThat(message.getGtsBulletinHeading().getBulletinHeading()).isPresent();
            assertThat(message.getGtsBulletinHeading().getBulletinHeadingString()).contains("FTYU31 YUDO 160000");
            assertThat(message.getCollectIdentifier().getBulletinHeading()).isEmpty();
            assertThat(message.getCollectIdentifier().getBulletinHeadingString()).isEmpty();
        });

        assertThat(result.getInputAviationMessages().get(0).getMessage().getOriginalMessage()).isEqualTo("TAF YUDO 160000Z NIL=");
        assertThat(result.getInputAviationMessages().get(1).getMessage().getOriginalMessage()).isEqualTo("TAF YUDD 160000Z NIL=");
    }

    @Test
    public void taf_tac_two_bulletins() {
        final String filename = "taf-tac-two-bulletins.bul";
        final FileParser.FileParseResult result = fileParser.parse(getFileContent(filename), filename, DEFAULT_FILENAME_PATTERN, FILE_MODIFIED,
                PRODUCT_IDENTIFIER, TAC);

        assertThat(result.getParseErrors()).isFalse();
        assertThat(result.getInputAviationMessages()).hasSize(4);

        assertThat(result.getInputAviationMessages()).allSatisfy(message -> {
            assertThat(message.getGtsBulletinHeading().getBulletinHeading()).isPresent();
            assertThat(message.getCollectIdentifier().getBulletinHeading()).isEmpty();
            assertThat(message.getCollectIdentifier().getBulletinHeadingString()).isEmpty();
        });

        assertThat(result.getInputAviationMessages().subList(0, 2)).allSatisfy(
                message -> assertThat(message.getGtsBulletinHeading().getBulletinHeadingString()).contains("FTYU31 YUDO 160000"));
        assertThat(result.getInputAviationMessages().subList(2, 4)).allSatisfy(
                message -> assertThat(message.getGtsBulletinHeading().getBulletinHeadingString()).contains("FTYU31 YUDO 180000"));

        assertThat(result.getInputAviationMessages().get(0).getMessage().getOriginalMessage()).isEqualTo("TAF YUDO 160000Z NIL=");
        assertThat(result.getInputAviationMessages().get(1).getMessage().getOriginalMessage()).isEqualTo("TAF YUDD 160000Z NIL=");
    }

    @Test
    public void taf_iwxxm() {
        final String filename = "taf.xml";
        final String fileContent = getFileContent(filename);
        final FileParser.FileParseResult result = fileParser.parse(fileContent, filename, DEFAULT_FILENAME_PATTERN, FILE_MODIFIED, PRODUCT_IDENTIFIER, IWXXM);

        assertThat(result.getParseErrors()).isFalse();
        assertThat(result.getInputAviationMessages()).hasSize(1);

        final InputAviationMessage message = result.getInputAviationMessages().get(0);
        assertThat(message.getGtsBulletinHeading().getBulletinHeading()).isEmpty();
        assertThat(message.getGtsBulletinHeading().getBulletinHeadingString()).isEmpty();
        assertThat(message.getCollectIdentifier().getBulletinHeading()).isEmpty();
        assertThat(message.getCollectIdentifier().getBulletinHeadingString()).isEmpty();
        assertThat(message.getMessage().getMessageType()).contains(MessageType.TAF);
        assertThat(message.getMessage().getOriginalMessage()).isNotEmpty();
    }

    @Test
    public void taf_iwxxm_without_issue_and_valid_time_elements() {
        final String filename = "taf-missing-issue-valid-times.xml";
        final String fileContent = getFileContent(filename);
        final FileParser.FileParseResult result = fileParser.parse(fileContent, filename, DEFAULT_FILENAME_PATTERN, FILE_MODIFIED, PRODUCT_IDENTIFIER, IWXXM);

        assertThat(result.getParseErrors()).isFalse();
        assertThat(result.getInputAviationMessages()).hasSize(1);

        final InputAviationMessage message = result.getInputAviationMessages().get(0);
        assertThat(message.getGtsBulletinHeading().getBulletinHeading()).isEmpty();
        assertThat(message.getGtsBulletinHeading().getBulletinHeadingString()).isEmpty();
        assertThat(message.getCollectIdentifier().getBulletinHeading()).isEmpty();
        assertThat(message.getCollectIdentifier().getBulletinHeadingString()).isEmpty();
        assertThat(message.getMessage().getMessageType()).contains(MessageType.TAF);
        assertThat(message.getMessage().getOriginalMessage()).isNotEmpty();
    }

    @Test
    public void taf_iwxxm_invalid() {
        final String filename = "taf-invalid-content.xml";
        final String fileContent = getFileContent(filename);
        assertThrows(IllegalStateException.class,
                () -> fileParser.parse(fileContent, filename, DEFAULT_FILENAME_PATTERN, FILE_MODIFIED, PRODUCT_IDENTIFIER, IWXXM));
    }

    @Test
    public void taf_iwxxm_with_gts_heading() {
        final String filename = "taf-gts-heading.xml";
        final String fileContent = getFileContent(filename);
        final FileParser.FileParseResult result = fileParser.parse(fileContent, filename, DEFAULT_FILENAME_PATTERN, FILE_MODIFIED, PRODUCT_IDENTIFIER, IWXXM);

        assertThat(result.getParseErrors()).isFalse();
        assertThat(result.getInputAviationMessages()).hasSize(1);

        final InputAviationMessage message = result.getInputAviationMessages().get(0);
        assertThat(message.getGtsBulletinHeading().getBulletinHeading()).isPresent();
        assertThat(message.getGtsBulletinHeading().getBulletinHeadingString()).contains("LTFI31 EFKL 301115");
        assertThat(message.getCollectIdentifier().getBulletinHeading()).isEmpty();
        assertThat(message.getCollectIdentifier().getBulletinHeadingString()).isEmpty();
        assertThat(message.getMessage().getMessageType()).contains(MessageType.TAF);
        assertThat(message.getMessage().getOriginalMessage()).isNotEmpty();
    }

    @Test
    public void taf_iwxxm_bulletin() {
        final String filename = "taf-bulletin.xml";
        final String fileContent = getFileContent(filename);
        final FileParser.FileParseResult result = fileParser.parse(fileContent, filename, DEFAULT_FILENAME_PATTERN, FILE_MODIFIED, PRODUCT_IDENTIFIER, IWXXM);

        assertThat(result.getParseErrors()).isFalse();
        assertThat(result.getInputAviationMessages()).hasSize(2);

        assertThat(result.getInputAviationMessages()).allSatisfy(message -> {
            assertThat(message.getGtsBulletinHeading().getBulletinHeading()).isEmpty();
            assertThat(message.getGtsBulletinHeading().getBulletinHeadingString()).isEmpty();
            assertThat(message.getCollectIdentifier().getBulletinHeadingString()).contains("A_LTFI31EFKL301115_C_EFKL_201902011315--.xml");
            assertThat(message.getCollectIdentifier().getBulletinHeading()).isPresent();
            assertThat(message.getMessage().getMessageType()).contains(MessageType.TAF);
            assertThat(message.getMessage().getOriginalMessage()).isNotEmpty();
        });
    }

    @Test
    public void taf_iwxxm_bulletin_with_gts_heading() {
        final String filename = "taf-gts-heading-bulletin.xml";
        final String fileContent = getFileContent(filename);
        final FileParser.FileParseResult result = fileParser.parse(fileContent, filename, DEFAULT_FILENAME_PATTERN, FILE_MODIFIED, PRODUCT_IDENTIFIER, IWXXM);

        assertThat(result.getParseErrors()).isFalse();
        assertThat(result.getInputAviationMessages()).hasSize(2);

        assertThat(result.getInputAviationMessages()).allSatisfy(message -> {
            assertThat(message.getGtsBulletinHeading().getBulletinHeading()).isPresent();
            assertThat(message.getGtsBulletinHeading().getBulletinHeadingString()).contains("LTFI31 EFKL 301115");
            assertThat(message.getCollectIdentifier().getBulletinHeadingString()).contains("A_LTFI31EFKL301115_C_EFKL_201902011315--.xml");
            assertThat(message.getCollectIdentifier().getBulletinHeading()).isPresent();
            assertThat(message.getMessage().getMessageType()).contains(MessageType.TAF);
            assertThat(message.getMessage().getOriginalMessage()).isNotEmpty();
        });
    }

    @Test
    public void taf_iwxxm_in_gts_bulletin() {
        final String filename = "taf-iwxxm-bulletin.bul";
        final String fileContent = getFileContent(filename);
        final FileParser.FileParseResult result = fileParser.parse(fileContent, filename, DEFAULT_FILENAME_PATTERN, FILE_MODIFIED, PRODUCT_IDENTIFIER, IWXXM);

        assertThat(result.getParseErrors()).isFalse();
        assertThat(result.getInputAviationMessages()).hasSize(1);

        assertThat(result.getInputAviationMessages()).allSatisfy(message -> {
            assertThat(message.getGtsBulletinHeading().getBulletinHeading()).isPresent();
            assertThat(message.getGtsBulletinHeading().getBulletinHeadingString()).contains("LTFI31 EFKL 301115");
            assertThat(message.getCollectIdentifier().getBulletinHeadingString()).isEmpty();
            assertThat(message.getCollectIdentifier().getBulletinHeading()).isEmpty();
            assertThat(message.getMessage().getMessageType()).contains(MessageType.TAF);
            assertThat(message.getMessage().getOriginalMessage()).isNotEmpty();
        });
    }

    @Test
    public void taf_iwxxm_in_gts_bulletin_with_invalid_heading() {
        final String filename = "taf-iwxxm-in-gts-bulletin-with-invalid-heading.bul";
        final String fileContent = getFileContent(filename);
        assertThrows(IllegalStateException.class,
                () -> fileParser.parse(fileContent, filename, DEFAULT_FILENAME_PATTERN, FILE_MODIFIED, PRODUCT_IDENTIFIER, IWXXM));
    }

}
