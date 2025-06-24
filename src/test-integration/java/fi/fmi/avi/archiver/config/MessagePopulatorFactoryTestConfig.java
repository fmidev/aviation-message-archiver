package fi.fmi.avi.archiver.config;

import fi.fmi.avi.archiver.config.model.MessagePopulatorFactory;
import fi.fmi.avi.archiver.message.processor.populator.MessageAppendingPopulator;
import fi.fmi.avi.archiver.message.processor.populator.MessagePopulator;
import fi.fmi.avi.archiver.util.instantiation.ReflectionObjectFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import static java.util.Objects.requireNonNull;

@Configuration
@Profile({"integration-test"})
public class MessagePopulatorFactoryTestConfig {
    private final MessagePopulatorFactoryConfig messagePopulatorFactoryConfig;

    public MessagePopulatorFactoryTestConfig(final MessagePopulatorFactoryConfig messagePopulatorFactoryConfig) {
        this.messagePopulatorFactoryConfig = requireNonNull(messagePopulatorFactoryConfig, "messagePopulatorFactoryConfig");
    }

    private <T extends MessagePopulator> ReflectionObjectFactory.Builder<T> builder(final Class<T> type) {
        return messagePopulatorFactoryConfig.builder(type);
    }

    private <T extends MessagePopulator> MessagePopulatorFactory<T> build(final ReflectionObjectFactory.Builder<T> builder) {
        return messagePopulatorFactoryConfig.build(builder);
    }

    @Bean
    MessagePopulatorFactory<MessageAppendingPopulator> messageAppendingPopulator() {
        return build(builder(MessageAppendingPopulator.class)
                .addConfigArg("content", String.class));
    }
}
