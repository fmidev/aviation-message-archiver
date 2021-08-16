package fi.fmi.avi.archiver.config;

import java.util.Collections;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fi.fmi.avi.model.GenericAviationWeatherMessage;
import fi.fmi.avi.model.MessageType;

/**
 * Holder for static values of messages.
 */
@Configuration
@EnableConfigurationProperties(MessageStaticValuesConfig.class)
@ConfigurationProperties(prefix = "message-static-values")
public class MessageStaticValuesConfig {
    private Map<String, Integer> routeIds;
    private Map<GenericAviationWeatherMessage.Format, Integer> formatIds;
    private Map<MessageType, Integer> typeIds;

    Map<String, Integer> getRouteIds() {
        return routeIds;
    }

    void setRouteIds(final Map<String, Integer> routeIds) {
        this.routeIds = routeIds;
    }

    Map<GenericAviationWeatherMessage.Format, Integer> getFormatIds() {
        return formatIds;
    }

    void setFormatIds(final Map<GenericAviationWeatherMessage.Format, Integer> formatIds) {
        this.formatIds = formatIds;
    }

    Map<MessageType, Integer> getTypeIds() {
        return typeIds;
    }

    void setTypeIds(final Map<MessageType, Integer> typeIds) {
        this.typeIds = typeIds;
    }

    @Bean
    public Map<String, Integer> messageRouteIds() {
        return Collections.unmodifiableMap(routeIds);
    }

    @Bean
    public Map<GenericAviationWeatherMessage.Format, Integer> messageFormatIds() {
        return Collections.unmodifiableMap(formatIds);
    }

    @Bean
    public Map<MessageType, Integer> messageTypeIds() {
        return Collections.unmodifiableMap(typeIds);
    }
}
