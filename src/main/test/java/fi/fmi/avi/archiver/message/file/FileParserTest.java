package fi.fmi.avi.archiver.message.file;

import fi.fmi.avi.archiver.config.ConverterConfig;
import fi.fmi.avi.archiver.file.FileParser;
import fi.fmi.avi.converter.AviMessageConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(ConverterConfig.class)
public class FileParserTest {

    @Autowired
    private AviMessageConverter aviMessageConverter;

    private FileParser fileParser;

    @BeforeEach
    public void setUp() {
        fileParser = new FileParser(aviMessageConverter);
    }

    @Test
    public void blah() {
        fileParser.parse("blah blah", null);
    }

}
