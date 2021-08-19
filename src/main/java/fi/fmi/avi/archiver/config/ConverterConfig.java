package fi.fmi.avi.archiver.config;

import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.AviMessageSpecificConverter;
import fi.fmi.avi.converter.iwxxm.conf.IWXXMConverter;
import fi.fmi.avi.converter.tac.conf.TACConverter;
import fi.fmi.avi.model.GenericAviationWeatherMessage;
import fi.fmi.avi.model.bulletin.GenericMeteorologicalBulletin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.w3c.dom.Document;

@Configuration
@Import({TACConverter.class, IWXXMConverter.class})
public class ConverterConfig {

    @Autowired
    private AviMessageSpecificConverter<String, GenericMeteorologicalBulletin> genericBulletinTACParser;

    @Autowired
    private AviMessageSpecificConverter<String, GenericAviationWeatherMessage> genericAviationWeatherMessageTACParser;

    @Autowired
    private AviMessageSpecificConverter<Document, GenericMeteorologicalBulletin> genericBulletinIWXXMDOMParser;

    @Autowired
    private AviMessageSpecificConverter<Document, GenericAviationWeatherMessage> genericAviationWeatherMessageIWXXMDOMParser;

    @Bean
    public AviMessageConverter aviMessageConverter() {
        final AviMessageConverter aviMessageConverter = new AviMessageConverter();
        aviMessageConverter.setMessageSpecificConverter(TACConverter.TAC_TO_GENERIC_BULLETIN_POJO, genericBulletinTACParser);
        aviMessageConverter.setMessageSpecificConverter(TACConverter.TAC_TO_GENERIC_AVIATION_WEATHER_MESSAGE_POJO, genericAviationWeatherMessageTACParser);
        aviMessageConverter.setMessageSpecificConverter(IWXXMConverter.WMO_COLLECT_DOM_TO_GENERIC_BULLETIN_POJO, genericBulletinIWXXMDOMParser);
        aviMessageConverter.setMessageSpecificConverter(IWXXMConverter.IWXXM_DOM_TO_GENERIC_AVIATION_WEATHER_MESSAGE_POJO, genericAviationWeatherMessageIWXXMDOMParser);
        return aviMessageConverter;
    }

}
