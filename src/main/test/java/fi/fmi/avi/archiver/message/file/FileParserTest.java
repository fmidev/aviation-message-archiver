package fi.fmi.avi.archiver.message.file;

import com.google.common.collect.ImmutableMap;
import fi.fmi.avi.archiver.config.ConverterConfig;
import fi.fmi.avi.archiver.file.FileParser;
import fi.fmi.avi.archiver.file.FilenamePattern;
import fi.fmi.avi.archiver.initializing.MessageFileMonitorInitializer;
import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.model.GenericAviationWeatherMessage;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.file.FileHeaders;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.Instant;
import java.util.Map;
import java.util.regex.Pattern;

@SpringJUnitConfig(ConverterConfig.class)
public class FileParserTest {

    private static final Map<String, Object> headers = ImmutableMap.of(
            FileHeaders.FILENAME, "test_file",
            MessageFileMonitorInitializer.MESSAGE_FILE_PATTERN, new FilenamePattern("", Pattern.compile("")),
            MessageFileMonitorInitializer.FILE_MODIFIED, Instant.now(),
            MessageFileMonitorInitializer.PRODUCT_IDENTIFIER, "test",
            MessageFileMonitorInitializer.FILE_FORMAT, GenericAviationWeatherMessage.Format.TAC
    );

    @Autowired
    private AviMessageConverter aviMessageConverter;

    private FileParser fileParser;

    @BeforeEach
    public void setUp() {
        fileParser = new FileParser(aviMessageConverter);
    }

}
