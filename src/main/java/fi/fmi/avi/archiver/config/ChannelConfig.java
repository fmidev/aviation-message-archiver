package fi.fmi.avi.archiver.config;

import java.util.concurrent.Executors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.messaging.MessageChannel;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

@Configuration
public class ChannelConfig {

    @Autowired
    private ThreadGroup aviationMessageArchiverThreadGroup;

    @Bean
    public MessageChannel processingChannel() {
        return new PublishSubscribeChannel(Executors.newSingleThreadExecutor(newThreadFactory("Processing-")));
    }

    @Bean
    public MessageChannel archiveChannel() {
        return new PublishSubscribeChannel(Executors.newSingleThreadExecutor(newThreadFactory("Archive-")));
    }

    @Bean
    public MessageChannel failChannel() {
        return new PublishSubscribeChannel(Executors.newSingleThreadExecutor(newThreadFactory("Fail-")));
    }

    @Bean
    public MessageChannel parserChannel() {
        return new PublishSubscribeChannel(Executors.newSingleThreadExecutor(newThreadFactory("Parser-")));
    }

    @Bean
    public MessageChannel modifierChannel() {
        return new PublishSubscribeChannel(Executors.newSingleThreadExecutor(newThreadFactory("Modifier-")));
    }

    @Bean
    public MessageChannel validatorChannel() {
        return new PublishSubscribeChannel(Executors.newSingleThreadExecutor(newThreadFactory("Validator-")));
    }

    @Bean
    public MessageChannel errorMessageChannel() {
        return new PublishSubscribeChannel(Executors.newCachedThreadPool(newThreadFactory("Error-")));
    }

    private CustomizableThreadFactory newThreadFactory(final String threadNamePrefix) {
        final CustomizableThreadFactory threadFactory = new CustomizableThreadFactory(threadNamePrefix);
        threadFactory.setThreadGroup(aviationMessageArchiverThreadGroup);
        return threadFactory;
    }

}
