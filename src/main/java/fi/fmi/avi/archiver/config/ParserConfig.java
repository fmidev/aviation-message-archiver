package fi.fmi.avi.archiver.config;

import static java.util.Objects.requireNonNull;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;

import fi.fmi.avi.archiver.database.DatabaseAccess;
import fi.fmi.avi.archiver.file.FileMetadata;
import fi.fmi.avi.archiver.file.FileParser;
import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.initializing.MessageFileMonitorInitializer;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.populator.BulletinHeadingDataPopulator;
import fi.fmi.avi.archiver.message.populator.FileMetadataPopulator;
import fi.fmi.avi.archiver.message.populator.MessageDataPopulator;
import fi.fmi.avi.archiver.message.populator.MessagePopulator;
import fi.fmi.avi.archiver.message.populator.MessagePopulatorService;
import fi.fmi.avi.archiver.message.populator.StationIdPopulator;
import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.model.GenericAviationWeatherMessage;
import fi.fmi.avi.model.MessageType;

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

    @SuppressWarnings("FieldMayBeFinal")
    @Autowired(required = false)
    private List<MessagePopulator> messagePopulators = new ArrayList<>();

    @Autowired
    private Clock clock;

    @Autowired
    private DatabaseAccess databaseAccess;

    @Autowired
    private AviMessageConverter aviMessageConverter;

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
                .handle(fileParserService())//
                .<List<ArchiveAviationMessage>> filter(messages -> !messages.isEmpty(), discards -> discards.discardChannel(failChannel))//
                .channel(populatorChannel)//
                .handle(messagePopulatorService())//
                .channel(databaseChannel)//
                .get();
    }

    @PostConstruct
    private void addBasePopulators() {
        messagePopulators.addAll(0, Arrays.asList(//
                new FileMetadataPopulator(), //
                new BulletinHeadingDataPopulator(messageFormatIds, messageTypeIds,
                        Arrays.asList(BulletinHeadingDataPopulator.BulletinHeadingSource.GTS_BULLETIN_HEADING,
                                BulletinHeadingDataPopulator.BulletinHeadingSource.COLLECT_IDENTIFIER)), // TODO: make configurable in application.yml
                new MessageDataPopulator(messageFormatIds, messageTypeIds)));
        messagePopulators.add(new StationIdPopulator(databaseAccess));
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
