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
class ChannelConfig {

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
    ExecutorService processingExecutor() {
        return newBlockingSingleThreadExecutor("Processing-");
    }

    @Bean
    MessageChannel processingChannel() {
        return new PublishSubscribeChannel(processingExecutor());
    }

    @Bean
    ExecutorService archiveExecutor() {
        return newBlockingSingleThreadExecutor("Archive-");
    }

    @Bean
    MessageChannel archiveChannel() {
        return new PublishSubscribeChannel(archiveExecutor());
    }

    @Bean
    ExecutorService successExecutor() {
        return newBlockingSingleThreadExecutor("Success-");
    }

    @Bean
    MessageChannel successChannel() {
        return new PublishSubscribeChannel(successExecutor());
    }

    @Bean
    ExecutorService failExecutor() {
        return newBlockingSingleThreadExecutor("Fail-");
    }

    @Bean
    MessageChannel failChannel() {
        return new PublishSubscribeChannel(failExecutor());
    }

    @Bean
    ExecutorService parserExecutor() {
        return newBlockingSingleThreadExecutor("Parser-");
    }

    @Bean
    MessageChannel parserChannel() {
        return new PublishSubscribeChannel(parserExecutor());
    }

    @Bean
    ExecutorService populatorExecutor() {
        return newBlockingSingleThreadExecutor("Populator-");
    }

    @Bean
    MessageChannel populatorChannel() {
        return new PublishSubscribeChannel(populatorExecutor());
    }

    @Bean
    ExecutorService databaseExecutor() {
        return newBlockingSingleThreadExecutor("Database-");
    }

    @Bean
    MessageChannel databaseChannel() {
        return new PublishSubscribeChannel(databaseExecutor());
    }

    @Bean
    ExecutorService finishExecutor() {
        return newBlockingSingleThreadExecutor("Finish-");
    }

    @Bean
    MessageChannel finishChannel() {
        return new PublishSubscribeChannel(finishExecutor());
    }

    @Bean
    ExecutorService errorMessageExecutor() {
        return newBlockingSingleThreadExecutor("Error-");
    }

    @Bean
    MessageChannel errorMessageChannel() {
        return new PublishSubscribeChannel(errorMessageExecutor());
    }

    @Bean
    ExecutorService errorLoggingExecutor() {
        return Executors.newCachedThreadPool(newThreadFactory("Error-Log-"));
    }

    @Bean
    MessageChannel errorLoggingChannel() {
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
