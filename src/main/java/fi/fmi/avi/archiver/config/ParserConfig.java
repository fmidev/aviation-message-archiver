package fi.fmi.avi.archiver.config;

import fi.fmi.avi.archiver.database.DatabaseAccess;
import fi.fmi.avi.archiver.file.FileMetadata;
import fi.fmi.avi.archiver.file.FileParser;
import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.initializing.AviationProductsHolder;
import fi.fmi.avi.archiver.initializing.MessageFileMonitorInitializer;
import fi.fmi.avi.archiver.message.populator.MessagePopulator;
import fi.fmi.avi.archiver.message.populator.MessagePopulatorService;
import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.model.GenericAviationWeatherMessage;
import fi.fmi.avi.model.MessageType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;

import javax.annotation.Resource;
import java.time.Clock;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;

@Configuration
public class ParserConfig {

    @Autowired
    private MessageChannel parserChannel;

    @Autowired
    private MessageChannel populatorChannel;

    @Autowired
    private MessageChannel databaseChannel;

    @Autowired
    private MessageChannel failChannel;

    @Qualifier("messagePopulators")
    @Autowired
    private List<MessagePopulator> messagePopulators;

    @Autowired
    private Clock clock;

    @Autowired
    private DatabaseAccess databaseAccess;

    @Autowired
    private AviMessageConverter aviMessageConverter;

    @Autowired
    private AviationProductsHolder aviationProductsHolder;

    @Resource(name = "messageTypeIds")
    private Map<MessageType, Integer> messageTypeIds;

    @Resource(name = "messageFormatIds")
    private Map<GenericAviationWeatherMessage.Format, Integer> messageFormatIds;

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

    @Bean
    public IntegrationFlow parserFlow() {
        return IntegrationFlows.from(parserChannel)//
                .<String>filter(content -> content != null && !content.isEmpty(), discards -> discards.discardChannel(failChannel))
                .handle(fileParserService())//
                .<List<InputAviationMessage>>filter(messages -> !messages.isEmpty(), discards -> discards.discardChannel(failChannel))
                .channel(populatorChannel)//
                .handle(messagePopulatorService())//
                .channel(databaseChannel)//
                .get();
    }

    private static class FileParserService {
        private final FileParser fileParser;

        FileParserService(final FileParser fileParser) {
            this.fileParser = requireNonNull(fileParser, "fileParser");
        }

        @ServiceActivator
        Message<List<InputAviationMessage>> parse(final String content, final MessageHeaders headers) {
            final FileMetadata fileMetadata = headers.get(MessageFileMonitorInitializer.FILE_METADATA, FileMetadata.class);
            requireNonNull(fileMetadata, "fileMetadata");
            final FileParser.FileParseResult result = fileParser.parse(content, fileMetadata);
            return MessageBuilder.withPayload(result.getInputAviationMessages())
                    .copyHeaders(headers)
                    .setHeader(MessageFileMonitorInitializer.FILE_PARSE_ERRORS, result.getParseErrors())
                    .build();
        }
    }

}
