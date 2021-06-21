package fi.fmi.avi.archiver.config;

import fi.fmi.avi.archiver.database.DatabaseAccess;
import fi.fmi.avi.archiver.file.FileParser;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.modifier.BaseDataPopulator;
import fi.fmi.avi.archiver.message.modifier.MessageModifierService;
import fi.fmi.avi.archiver.message.modifier.MessagePopulator;
import fi.fmi.avi.archiver.message.modifier.StationIdPopulator;
import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.AviMessageSpecificConverter;
import fi.fmi.avi.converter.tac.conf.TACConverter;
import fi.fmi.avi.model.MessageType;
import fi.fmi.avi.model.bulletin.GenericMeteorologicalBulletin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.messaging.MessageChannel;

import javax.annotation.PostConstruct;
import java.time.Clock;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Configuration
@EnableConfigurationProperties(ParserConfig.class)
@ConfigurationProperties("message-parsing")
@Import(TACConverter.class)
public class ParserConfig {

    private ZoneId zone;

    private Map<MessageType, Integer> types;

    public Map<MessageType, Integer> getTypes() {
        return types;
    }

    public void setTypes(final Map<MessageType, Integer> types) {
        this.types = types;
    }

    @Autowired
    private AviMessageSpecificConverter<String, GenericMeteorologicalBulletin> genericBulletinTACParser;

    @Autowired
    private MessageChannel parserChannel;

    @Autowired
    private MessageChannel modifierChannel;

    @Autowired
    private MessageChannel validatorChannel;

    @Autowired
    private MessageChannel failChannel;

    @SuppressWarnings("FieldMayBeFinal")
    @Autowired(required = false)
    private List<MessagePopulator> messagePopulators = new ArrayList<>();

    @Autowired
    private Clock clock;

    @Autowired
    private DatabaseAccess databaseAccess;

    public ZoneId getZone() {
        return zone;
    }

    public void setZone(final ZoneId zone) {
        this.zone = zone;
    }

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
    public MessageModifierService messageModifierService() {
        return new MessageModifierService(messagePopulators);
    }

    @Bean
    public IntegrationFlow parserFlow() {
        return IntegrationFlows.from(parserChannel)//
                .handle(fileParser())//
                .<List<ArchiveAviationMessage>>filter(messages -> !messages.isEmpty(), discards -> discards.discardChannel(failChannel))//
                .channel(modifierChannel)//
                .handle(messageModifierService())//
                .channel(validatorChannel)//
                .get();
    }

    @PostConstruct
    private void addBaseModifiers() {
        messagePopulators.add(0, new BaseDataPopulator(clock, types));
        messagePopulators.add(1, new StationIdPopulator(databaseAccess));
    }

}
