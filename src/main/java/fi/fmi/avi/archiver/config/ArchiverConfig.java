package fi.fmi.avi.archiver.config;

import java.time.Clock;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.scheduling.PollerMetadata;

import fi.fmi.avi.archiver.ProcessingState;
import fi.fmi.avi.archiver.spring.context.CompoundLifecycle;
import fi.fmi.avi.archiver.spring.context.GracefulShutdownManager;

@Configuration
public class ArchiverConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public CompoundLifecycle inputReadersLifecycle() {
        return new CompoundLifecycle();
    }

    @Bean
    public GracefulShutdownManager shutdownManager(@Value("${processing-flow.gracefulShutdown.timeout:PT20S}") final Duration gracefulShutdownTimeout,
            @Value("${processing-flow.gracefulShutdown.pollingInterval:PT0.1S}") final Duration gracefulShutdownPollingInterval) {
        final ProcessingState processingState = processingState();
        final GracefulShutdownManager shutdownManager = new GracefulShutdownManager(inputReadersLifecycle(),
                () -> processingState.getFileCountUnderProcessing() > 0);
        shutdownManager.setTimeout(gracefulShutdownTimeout);
        shutdownManager.setPollingInterval(gracefulShutdownPollingInterval);
        return shutdownManager;
    }

    @Bean
    public ProcessingState processingState() {
        return new ProcessingState(clock());
    }

    @Bean(name = PollerMetadata.DEFAULT_POLLER)
    public PollerMetadata poller(@Value("${polling.delay}") final int pollingDelay) {
        return Pollers.fixedDelay(pollingDelay).get();
    }

}
