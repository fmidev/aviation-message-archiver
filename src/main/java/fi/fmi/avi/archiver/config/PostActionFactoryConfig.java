package fi.fmi.avi.archiver.config;

import com.google.common.collect.BiMap;
import fi.fmi.avi.archiver.config.factory.postaction.SwimRabbitMQPublisherFactory;
import fi.fmi.avi.archiver.config.model.PostActionFactory;
import fi.fmi.avi.archiver.message.processor.postaction.ResultLogger;
import fi.fmi.avi.archiver.message.processor.postaction.SwimRabbitMQPublisher;
import fi.fmi.avi.archiver.spring.healthcontributor.SwimRabbitMQConnectionHealthContributor;
import fi.fmi.avi.archiver.util.instantiation.ConfigValueConverter;
import fi.fmi.avi.archiver.util.instantiation.ObjectFactoryConfigFactory;
import fi.fmi.avi.model.GenericAviationWeatherMessage;
import fi.fmi.avi.model.MessageType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.util.Map;

@Configuration
public class PostActionFactoryConfig extends AbstractPostActionFactoryConfig {

    PostActionFactoryConfig(final ConfigValueConverter configValueConverter) {
        super(configValueConverter);
    }

    @Bean
    PostActionFactory<ResultLogger> resultLoggerPostActionFactory() {
        return build(builder(ResultLogger.class)
                .addConfigArg("message", String.class));
    }

    @Bean
    PostActionFactory<SwimRabbitMQPublisher> swimRabbitMQPublisherPostActionFactory(
            final ObjectFactoryConfigFactory objectFactoryConfigFactory,
            final SwimRabbitMQConnectionHealthContributor swimRabbitMQConnectionHealthContributor,
            final Clock clock,
            final Map<GenericAviationWeatherMessage.Format, Integer> messageFormatIds,
            final BiMap<MessageType, Integer> messageTypeIds
    ) {
        return decorateAutoCloseable(new SwimRabbitMQPublisherFactory(
                objectFactoryConfigFactory,
                swimRabbitMQConnectionHealthContributor,
                clock,
                messageFormatIds.get(GenericAviationWeatherMessage.Format.IWXXM),
                messageTypeIds
        ));
    }
}
