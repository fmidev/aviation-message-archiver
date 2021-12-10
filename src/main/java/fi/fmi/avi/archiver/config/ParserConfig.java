package fi.fmi.avi.archiver.config;

import fi.fmi.avi.archiver.file.FileMetadata;
import fi.fmi.avi.archiver.file.FileParser;
import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.populator.MessagePopulator;
import fi.fmi.avi.archiver.message.populator.MessagePopulatorService;
import fi.fmi.avi.converter.AviMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

import java.util.List;

import static java.util.Objects.requireNonNull;

@Configuration
public class ParserConfig {

    @Qualifier("messagePopulators")
    @Autowired
    private List<MessagePopulator> messagePopulators;

    @Autowired
    private AviMessageConverter aviMessageConverter;

    @Bean
    public FileParser fileParser() {
        return new FileParser(aviMessageConverter);
    }

    @Bean
    public FileParserService fileParserService() {
        return new FileParserService(fileParser());
    }

    @Bean
    public MessagePopulatorService messagePopulatorService() {
        return new MessagePopulatorService(messagePopulators);
    }

    public static class FileParserService {
        private final FileParser fileParser;

        FileParserService(final FileParser fileParser) {
            this.fileParser = requireNonNull(fileParser, "fileParser");
        }

        public Message<List<InputAviationMessage>> parse(final String content, final MessageHeaders headers) {
            final FileMetadata fileMetadata = headers.get(IntegrationFlowConfig.FILE_METADATA, FileMetadata.class);
            requireNonNull(fileMetadata, "fileMetadata");
            final FileParser.FileParseResult result = fileParser.parse(content, fileMetadata);
            return MessageBuilder.withPayload(result.getInputAviationMessages())
                    .copyHeaders(headers)
                    .setHeader(IntegrationFlowConfig.FILE_PARSE_ERRORS, result.getParseErrors())
                    .build();
        }
    }

}
