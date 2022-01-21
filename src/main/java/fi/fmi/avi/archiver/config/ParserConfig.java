package fi.fmi.avi.archiver.config;

import static fi.fmi.avi.archiver.config.IntegrationFlowConfig.FILE_METADATA;
import static java.util.Objects.requireNonNull;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

import fi.fmi.avi.archiver.file.FileMetadata;
import fi.fmi.avi.archiver.file.FileParser;
import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.logging.LoggingContext;
import fi.fmi.avi.converter.AviMessageConverter;

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

        public Message<List<InputAviationMessage>> parse(final String content, final MessageHeaders headers) {
            final FileMetadata fileMetadata = requireNonNull(headers.get(FILE_METADATA, FileMetadata.class), "fileMetadata");
            final LoggingContext loggingContext = SpringLoggingContextHelper.getLoggingContext(headers);
            final FileParser.FileParseResult result = fileParser.parse(content, fileMetadata, loggingContext);
            return MessageBuilder.withPayload(result.getInputAviationMessages())
                    .copyHeaders(headers)
                    .setHeader(IntegrationFlowConfig.PROCESSING_ERRORS, IntegrationFlowConfig.hasProcessingErrors(headers) || result.getParseErrors())
                    .build();
        }
    }

}
