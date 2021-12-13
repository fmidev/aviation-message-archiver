package fi.fmi.avi.archiver.config;

import fi.fmi.avi.archiver.AviationMessageArchiver;
import fi.fmi.avi.archiver.spring.healthcontributor.BlockingExecutorHealthContributor;
import fi.fmi.avi.archiver.spring.integration.util.MonitorableCallerBlocksPolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.messaging.MessageChannel;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

import java.time.Clock;
import java.util.concurrent.*;

import static java.util.Objects.requireNonNull;

@Configuration
public class ChannelConfig {

    private final Clock clock;
    private final BlockingExecutorHealthContributor executorHealthContributor;
    private final int executorQueueSize;

    public ChannelConfig(final Clock clock, final BlockingExecutorHealthContributor executorHealthContributor,
                         @Value("${executor.queue-size}") final int executorQueueSize) {
        this.clock = requireNonNull(clock, "clock");
        this.executorHealthContributor = requireNonNull(executorHealthContributor, "executorHealthContributor");
        this.executorQueueSize = executorQueueSize;
    }

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

    @Bean(destroyMethod = "destroy")
    public ThreadGroup aviationMessageArchiverThreadGroup() {
        return new ThreadGroup(AviationMessageArchiver.class.getSimpleName());
    }

    private ExecutorService newBlockingSingleThreadExecutor(final String threadNamePrefix) {
        final BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(executorQueueSize);
        final MonitorableCallerBlocksPolicy callerBlocksPolicy = new MonitorableCallerBlocksPolicy(clock, Long.MAX_VALUE);
        executorHealthContributor.register(threadNamePrefix + "executor", callerBlocksPolicy);
        return new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, queue,
                newThreadFactory(threadNamePrefix), callerBlocksPolicy);
    }

    private CustomizableThreadFactory newThreadFactory(final String threadNamePrefix) {
        final CustomizableThreadFactory threadFactory = new CustomizableThreadFactory(threadNamePrefix);
        threadFactory.setThreadGroup(aviationMessageArchiverThreadGroup());
        return threadFactory;
    }

}
