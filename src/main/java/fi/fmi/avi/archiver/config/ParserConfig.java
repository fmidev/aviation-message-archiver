package fi.fmi.avi.archiver.config;

import fi.fmi.avi.archiver.ProcessingServiceContext;
import fi.fmi.avi.archiver.config.util.SpringProcessingServiceContextHelper;
import fi.fmi.avi.archiver.file.FileMetadata;
import fi.fmi.avi.archiver.file.FileParser;
import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.converter.AviMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.MessageHeaders;

import java.util.List;

import static fi.fmi.avi.archiver.config.IntegrationFlowConfig.FILE_METADATA;
import static java.util.Objects.requireNonNull;

@Configuration
public class ParserConfig {

    @Bean
    FileParser fileParser(final AviMessageConverter aviMessageConverter) {
        return new FileParser(aviMessageConverter);
    }

    @Bean
    FileParserIntegrationService fileParserIntegrationService(final FileParser fileParser) {
        return new FileParserIntegrationService(fileParser);
    }

    public static class FileParserIntegrationService {
        private final FileParser fileParser;

        FileParserIntegrationService(final FileParser fileParser) {
            this.fileParser = requireNonNull(fileParser, "fileParser");
        }

        public List<InputAviationMessage> parse(final String fileContent, final MessageHeaders headers) {
            final FileMetadata fileMetadata = FILE_METADATA.getNonNull(headers);
            final ProcessingServiceContext context = SpringProcessingServiceContextHelper.getProcessingServiceContext(headers);
            return fileParser.parse(fileContent, fileMetadata, context);
        }
    }

}
