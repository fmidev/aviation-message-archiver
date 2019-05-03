package fi.fmi.avi.archiver.config;

import java.time.Clock;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import fi.fmi.avi.archiver.message.MessageParser;
import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.AviMessageSpecificConverter;
import fi.fmi.avi.converter.tac.conf.TACConverter;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.GenericMeteorologicalBulletin;

@Configuration
@EnableConfigurationProperties(MessageParserConfig.class)
@ConfigurationProperties("message-parsing")
@Import(TACConverter.class)
public class MessageParserConfig {

    private Map<AviationCodeListUser.MessageType, Integer> types;
    @Autowired
    private AviMessageSpecificConverter<String, GenericMeteorologicalBulletin> genericBulletinTACParser;

    public Map<AviationCodeListUser.MessageType, Integer> getTypes() {
        return types;
    }

    public void setTypes(final Map<AviationCodeListUser.MessageType, Integer> types) {
        this.types = types;
    }

    // TODO Move this somewhere
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public AviMessageConverter aviMessageConverter() {
        final AviMessageConverter p = new AviMessageConverter();
        p.setMessageSpecificConverter(TACConverter.TAC_TO_GENERIC_BULLETIN_POJO, genericBulletinTACParser);
        return p;
    }

    @Bean
    public MessageParser messageParser() {
        return new MessageParser(clock(), aviMessageConverter(), types);
    }

}
