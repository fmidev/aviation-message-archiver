package fi.fmi.avi.archiver.config;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.messaging.MessageChannel;

import fi.fmi.avi.archiver.database.DatabaseAccess;
import fi.fmi.avi.archiver.file.FileParser;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.populator.BaseDataPopulator;
import fi.fmi.avi.archiver.message.populator.MessagePopulator;
import fi.fmi.avi.archiver.message.populator.MessagePopulatorService;
import fi.fmi.avi.archiver.message.populator.StationIdPopulator;
import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.AviMessageSpecificConverter;
import fi.fmi.avi.converter.tac.conf.TACConverter;
import fi.fmi.avi.model.MessageType;
import fi.fmi.avi.model.bulletin.GenericMeteorologicalBulletin;

@Configuration
@Import(TACConverter.class)
public class ParserConfig {

    @Autowired
    private AviMessageSpecificConverter<String, GenericMeteorologicalBulletin> genericBulletinTACParser;

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

    @Resource(name = "messageTypeIds")
    private Map<MessageType, Integer> messageTypeIds;

    @Bean
    public AviMessageConverter aviMessageConverter() {
        final AviMessageConverter aviMessageConverter = new AviMessageConverter();
        aviMessageConverter.setMessageSpecificConverter(TACConverter.TAC_TO_GENERIC_BULLETIN_POJO, genericBulletinTACParser);
        return aviMessageConverter;
    }

    @Bean
    public FileParser fileParser() {
        return new FileParser(aviMessageConverter());
    }

    @Bean
    public MessagePopulatorService messagePopulatorService() {
        return new MessagePopulatorService(messagePopulators);
    }

    @Bean
    public IntegrationFlow parserFlow() {
        return IntegrationFlows.from(parserChannel)//
                .handle(fileParser())//
                .<List<ArchiveAviationMessage>> filter(messages -> !messages.isEmpty(), discards -> discards.discardChannel(failChannel))//
                .channel(populatorChannel)//
                .handle(messagePopulatorService())//
                .channel(databaseChannel)//
                .get();
    }

    @PostConstruct
    private void addBasePopulators() {
        messagePopulators.add(0, new BaseDataPopulator(clock, messageTypeIds));
        messagePopulators.add(new StationIdPopulator(databaseAccess));
    }

}
