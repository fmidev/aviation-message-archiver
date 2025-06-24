package fi.fmi.avi.archiver.config;

import fi.fmi.avi.archiver.config.model.PostActionFactory;
import fi.fmi.avi.archiver.message.processor.postaction.ResultLogger;
import fi.fmi.avi.archiver.util.instantiation.ConfigValueConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
}
