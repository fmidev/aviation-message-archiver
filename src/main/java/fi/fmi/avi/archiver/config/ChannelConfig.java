package fi.fmi.avi.archiver.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.util.CallerBlocksPolicy;
import org.springframework.messaging.MessageChannel;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

import java.util.concurrent.*;

@Configuration
public class ChannelConfig {

    @Value("${executor.queue-size}")
    private int executorQueueSize;

    @Autowired
    private ThreadGroup aviationMessageArchiverThreadGroup;

    @Bean
    public ExecutorService processingExecutor() {
        return newBlockingSingleThreadExecutor("Processing-");
    }

    @Bean
    public MessageChannel processingChannel() {
        return new PublishSubscribeChannel(processingExecutor());
    }

    @Bean
    public ExecutorService archiveExecutor() {
        return newBlockingSingleThreadExecutor("Archive-");
    }

    @Bean
    public MessageChannel archiveChannel() {
        return new PublishSubscribeChannel(archiveExecutor());
    }

    @Bean
    public ExecutorService successExecutor() {
        return newBlockingSingleThreadExecutor("Success-");
    }

    @Bean
    public MessageChannel successChannel() {
        return new PublishSubscribeChannel(successExecutor());
    }

    @Bean
    public ExecutorService failExecutor() {
        return newBlockingSingleThreadExecutor("Fail-");
    }

    @Bean
    public MessageChannel failChannel() {
        return new PublishSubscribeChannel(failExecutor());
    }

    @Bean
    public ExecutorService parserExecutor() {
        return newBlockingSingleThreadExecutor("Parser-");
    }

    @Bean
    public MessageChannel parserChannel() {
        return new PublishSubscribeChannel(parserExecutor());
    }

    @Bean
    public ExecutorService populatorExecutor() {
        return newBlockingSingleThreadExecutor("Populator-");
    }

    @Bean
    public MessageChannel populatorChannel() {
        return new PublishSubscribeChannel(populatorExecutor());
    }

    @Bean
    public ExecutorService databaseExecutor() {
        return newBlockingSingleThreadExecutor("Database-");
    }

    @Bean
    public MessageChannel databaseChannel() {
        return new PublishSubscribeChannel(databaseExecutor());
    }

    @Bean
    public ExecutorService finishExecutor() {
        return newBlockingSingleThreadExecutor("Finish-");
    }

    @Bean
    public MessageChannel finishChannel() {
        return new PublishSubscribeChannel(finishExecutor());
    }

    @Bean
    public ExecutorService errorMessageExecutor() {
        return newBlockingSingleThreadExecutor("Error-");
    }

    @Bean
    public MessageChannel errorMessageChannel() {
        return new PublishSubscribeChannel(errorMessageExecutor());
    }

    @Bean
    public ExecutorService errorLoggingExecutor() {
        return Executors.newCachedThreadPool(newThreadFactory("Error-Log-"));
    }

    @Bean
    public MessageChannel errorLoggingChannel() {
        return new PublishSubscribeChannel(errorLoggingExecutor());
    }

    private ExecutorService newBlockingSingleThreadExecutor(final String threadNamePrefix) {
        final BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(executorQueueSize);
        return new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, queue,
                newThreadFactory(threadNamePrefix), new CallerBlocksPolicy(Long.MAX_VALUE));
    }

    private CustomizableThreadFactory newThreadFactory(final String threadNamePrefix) {
        final CustomizableThreadFactory threadFactory = new CustomizableThreadFactory(threadNamePrefix);
        threadFactory.setThreadGroup(aviationMessageArchiverThreadGroup);
        return threadFactory;
    }

}
