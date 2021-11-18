package fi.fmi.avi.archiver.config;

import java.time.Clock;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.messaging.MessageChannel;

import fi.fmi.avi.archiver.ProcessingMetrics;
import fi.fmi.avi.archiver.file.FileMetadata;
import fi.fmi.avi.archiver.initializing.AviationProductsHolder;
import fi.fmi.avi.archiver.initializing.MessageFileMonitorInitializer;
import fi.fmi.avi.archiver.spring.context.CompoundLifecycle;
import fi.fmi.avi.archiver.spring.context.GracefulShutdownManager;
import fi.fmi.avi.archiver.spring.integration.dsl.ServiceActivators;

@Configuration
public class DirectoryInspectionConfig {

    @Value("${processing-flow.gracefulShutdown.timeout:PT20S}")
    private Duration gracefulShutdownTimeout;

    @Value("${processing-flow.gracefulShutdown.pollingInterval:PT0.1S}")
    private Duration gracefulShutdownPollingInterval;

    @Autowired
    private IntegrationFlowContext flowContext;

    @Autowired
    private AviationProductsHolder aviationProductsHolder;

    @Autowired
    private MessageChannel processingChannel;

    @Autowired
    private MessageChannel archiveChannel;

    @Autowired
    private MessageChannel successChannel;

    @Autowired
    private MessageChannel failChannel;

    @Autowired
    private MessageChannel errorMessageChannel;

    @Autowired
    private MessageChannel finishChannel;

    @Autowired
    private Clock clock;

    @Bean
    public CompoundLifecycle inputReadersLifecycle() {
        return new CompoundLifecycle();
    }

    @Bean
    public GracefulShutdownManager shutdownManager() {
        final ProcessingMetrics processingMetrics = processingMetrics();
        final GracefulShutdownManager shutdownManager = new GracefulShutdownManager(inputReadersLifecycle(),
                () -> processingMetrics.getFileCountUnderProcessing() > 0);
        shutdownManager.setTimeout(gracefulShutdownTimeout);
        shutdownManager.setPollingInterval(gracefulShutdownPollingInterval);
        return shutdownManager;
    }

    @Bean
    public ProcessingMetrics processingMetrics() {
        return new ProcessingMetrics(clock);
    }

    @Bean(name = PollerMetadata.DEFAULT_POLLER)
    public PollerMetadata poller(@Value("${polling.delay}") final int pollingDelay) {
        return Pollers.fixedDelay(pollingDelay).get();
    }

    @Bean
    public MessageFileMonitorInitializer messageFileMonitorInitializer() {
        return new MessageFileMonitorInitializer(flowContext, inputReadersLifecycle(), processingMetrics(), aviationProductsHolder, processingChannel,
                successChannel, failChannel, finishChannel, errorMessageChannel);
    }

    @Bean
    public IntegrationFlow archiveFlow() {
        return IntegrationFlows.from(successChannel).log("Archive").get();
    }

    @Bean
    public IntegrationFlow archiveRouter() {
        return IntegrationFlows.from(archiveChannel)//
                .route("headers." + MessageFileMonitorInitializer.FAILED_MESSAGES + ".isEmpty()" //
                        + " and !headers." + MessageFileMonitorInitializer.FILE_PARSE_ERRORS, r -> r//
                        .channelMapping(true, successChannel)//
                        .channelMapping(false, failChannel)//
                ).get();
    }

    @Bean
    public IntegrationFlow finishFlow() {
        final ProcessingMetrics processingMetrics = processingMetrics();
        return IntegrationFlows.from(finishChannel)//
                .handle(ServiceActivators.peekHeader(FileMetadata.class, MessageFileMonitorInitializer.FILE_METADATA, processingMetrics::finish))//
                .nullChannel();
    }
}
