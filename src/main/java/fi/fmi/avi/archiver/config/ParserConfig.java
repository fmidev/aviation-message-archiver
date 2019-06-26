package fi.fmi.avi.archiver.config;

import java.time.Clock;
import java.time.ZoneId;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.messaging.MessageChannel;

import fi.fmi.avi.archiver.message.MessageParser;
import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.AviMessageSpecificConverter;
import fi.fmi.avi.converter.tac.conf.TACConverter;
import fi.fmi.avi.model.GenericMeteorologicalBulletin;
import fi.fmi.avi.model.MessageType;

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
    private Clock clock;

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
    public MessageParser messageParser() {
        return new MessageParser(clock, aviMessageConverter(), types);
    }

    @Bean
    public IntegrationFlow parserFlow() {
        return IntegrationFlows.from(parserChannel)//
                .handle(messageParser())//
                .channel(modifierChannel)//
                .get();
    }

}
