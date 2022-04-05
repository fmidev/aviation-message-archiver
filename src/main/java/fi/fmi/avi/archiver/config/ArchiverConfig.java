package fi.fmi.avi.archiver.config;

import java.time.Clock;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fi.fmi.avi.archiver.ProcessingState;
import fi.fmi.avi.archiver.spring.context.CompoundLifecycle;
import fi.fmi.avi.archiver.spring.context.GracefulShutdownManager;

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
    GracefulShutdownManager shutdownManager(@Value("${processing-flow.graceful-shutdown.timeout:PT20S}") final Duration gracefulShutdownTimeout,
            @Value("${processing-flow.graceful-shutdown.polling-interval:PT0.1S}") final Duration gracefulShutdownPollingInterval) {
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

}
