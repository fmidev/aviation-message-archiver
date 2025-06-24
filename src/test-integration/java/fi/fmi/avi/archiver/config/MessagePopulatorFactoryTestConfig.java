package fi.fmi.avi.archiver.config;

import fi.fmi.avi.archiver.config.model.MessagePopulatorFactory;
import fi.fmi.avi.archiver.message.processor.populator.MessageAppendingPopulator;
import fi.fmi.avi.archiver.util.instantiation.ConfigValueConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile({"integration-test"})
public class MessagePopulatorFactoryTestConfig extends AbstractMessagePopulatorFactoryConfig {
    MessagePopulatorFactoryTestConfig(final ConfigValueConverter configValueConverter) {
        super(configValueConverter);
    }

    @Bean
    MessagePopulatorFactory<MessageAppendingPopulator> messageAppendingPopulator() {
        return build(builder(MessageAppendingPopulator.class)
                .addConfigArg("content", String.class));
    }
}
