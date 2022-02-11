package fi.fmi.avi.archiver.config;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.context.annotation.Bean;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;

import fi.fmi.avi.model.GenericAviationWeatherMessage;
import fi.fmi.avi.model.MessageType;

/**
 * Holder for static values of messages.
 */
@ConstructorBinding
@ConfigurationProperties(prefix = "message-static-values")
public class MessageStaticValuesConfig {
    private final Map<String, Integer> routeIds;
    private final Map<GenericAviationWeatherMessage.Format, Integer> formatIds;
    private final Map<MessageType, Integer> typeIds;

    MessageStaticValuesConfig(final Map<String, Integer> routeIds, final Map<GenericAviationWeatherMessage.Format, Integer> formatIds,
            final Map<MessageType, Integer> typeIds) {
        this.routeIds = requireNonNull(routeIds, "routeIds");
        this.formatIds = requireNonNull(formatIds, "formatIds");
        this.typeIds = requireNonNull(typeIds, "typeIds");

        checkArgument(!routeIds.isEmpty(), "Invalid configuration: routeIds is empty");
        checkArgument(!formatIds.isEmpty(), "Invalid configuration: formatIds is empty");
        checkArgument(!typeIds.isEmpty(), "Invalid configuration: typeIds is empty");
    }

    @Bean
    BiMap<String, Integer> messageRouteIds() {
        return ImmutableBiMap.copyOf(routeIds);
    }

    @Bean
    BiMap<GenericAviationWeatherMessage.Format, Integer> messageFormatIds() {
        return ImmutableBiMap.copyOf(formatIds);
    }

    @Bean
    BiMap<MessageType, Integer> messageTypeIds() {
        return ImmutableBiMap.copyOf(typeIds);
    }
}
