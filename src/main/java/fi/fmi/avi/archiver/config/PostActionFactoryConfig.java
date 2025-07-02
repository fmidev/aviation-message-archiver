package fi.fmi.avi.archiver.config;

import fi.fmi.avi.archiver.config.factory.postaction.SwimRabbitMQPublisherFactory;
import fi.fmi.avi.archiver.config.model.PostActionFactory;
import fi.fmi.avi.archiver.message.processor.postaction.ResultLogger;
import fi.fmi.avi.archiver.message.processor.postaction.SwimRabbitMQPublisher;
import fi.fmi.avi.archiver.spring.healthcontributor.SwimRabbitMQConnectionHealthContributor;
import fi.fmi.avi.archiver.util.instantiation.ConfigValueConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

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
            final SwimRabbitMQConnectionHealthContributor swimRabbitMQConnectionHealthContributor, final Clock clock) {
        return new SwimRabbitMQPublisherFactory(swimRabbitMQConnectionHealthContributor, clock);
    }
}
