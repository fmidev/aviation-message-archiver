package fi.fmi.avi.archiver.config;

import fi.fmi.avi.archiver.ProcessingState;
import fi.fmi.avi.archiver.spring.context.CompoundLifecycle;
import fi.fmi.avi.archiver.spring.context.GracefulShutdownManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.scheduling.PollerMetadata;

import java.time.Clock;
import java.time.Duration;

@Configuration
public class ArchiverConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    CompoundLifecycle inputReadersLifecycle() {
        return new CompoundLifecycle();
    }

    @Bean
    GracefulShutdownManager shutdownManager(@Value("${processing-flow.gracefulShutdown.timeout:PT20S}") final Duration gracefulShutdownTimeout,
                                            @Value("${processing-flow.gracefulShutdown.pollingInterval:PT0.1S}") final Duration gracefulShutdownPollingInterval) {
        final ProcessingState processingState = processingState();
        final GracefulShutdownManager shutdownManager = new GracefulShutdownManager(inputReadersLifecycle(),
                () -> processingState.getFileCountUnderProcessing() > 0);
        shutdownManager.setTimeout(gracefulShutdownTimeout);
        shutdownManager.setPollingInterval(gracefulShutdownPollingInterval);
        return shutdownManager;
    }

    @Bean
    ProcessingState processingState() {
        return new ProcessingState(clock());
    }

    @Bean(name = PollerMetadata.DEFAULT_POLLER)
    PollerMetadata poller(@Value("${polling.delay}") final int pollingDelay) {
        return Pollers.fixedDelay(pollingDelay).get();
    }

}
