package fi.fmi.avi.archiver.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.messaging.MessageChannel;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

import java.util.concurrent.Executors;

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
    public MessageChannel successChannel() {
        return new PublishSubscribeChannel(Executors.newSingleThreadExecutor(newThreadFactory("Success-")));
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
    public MessageChannel populatorChannel() {
        return new PublishSubscribeChannel(Executors.newSingleThreadExecutor(newThreadFactory("Populator-")));
    }

    @Bean
    public MessageChannel databaseChannel() {
        return new PublishSubscribeChannel(Executors.newSingleThreadExecutor(newThreadFactory("Database-")));
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
