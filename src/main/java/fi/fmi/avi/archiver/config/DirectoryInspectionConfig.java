package fi.fmi.avi.archiver.config;

import fi.fmi.avi.archiver.ProcessingState;
import fi.fmi.avi.archiver.file.FileMetadata;
import fi.fmi.avi.archiver.initializing.AviationProductsHolder;
import fi.fmi.avi.archiver.initializing.MessageFileMonitorInitializer;
import fi.fmi.avi.archiver.spring.context.CompoundLifecycle;
import fi.fmi.avi.archiver.spring.context.GracefulShutdownManager;
import fi.fmi.avi.archiver.spring.integration.dsl.ServiceActivators;
import org.aopalliance.aop.Advice;
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

import java.time.Clock;
import java.time.Duration;

@Configuration
public class DirectoryInspectionConfig {

    @Value("${processing-flow.gracefulShutdown.timeout:PT20S}")
    private Duration gracefulShutdownTimeout;

    @Value("${processing-flow.gracefulShutdown.pollingInterval:PT0.1S}")
    private Duration gracefulShutdownPollingInterval;

    @Value("${polling.filter-queue-size}")
    private int filterQueueSize;

    @Autowired
    private IntegrationFlowContext flowContext;

    @Autowired
    private AviationProductsHolder aviationProductsHolder;

    @Autowired
    private Clock clock;

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
    private Advice archiveRetryAdvice;

    @Autowired
    private Advice failRetryAdvice;

    @Autowired
    private Advice exceptionTrapAdvice;

    @Autowired
    private MessageChannel finishChannel;

    @Bean
    public CompoundLifecycle inputReadersLifecycle() {
        return new CompoundLifecycle();
    }

    @Bean
    public GracefulShutdownManager shutdownManager() {
        final ProcessingState processingState = processingState();
        final GracefulShutdownManager shutdownManager = new GracefulShutdownManager(inputReadersLifecycle(),
                () -> processingState.getFileCountUnderProcessing() > 0);
        shutdownManager.setTimeout(gracefulShutdownTimeout);
        shutdownManager.setPollingInterval(gracefulShutdownPollingInterval);
        return shutdownManager;
    }

    @Bean
    public ProcessingState processingState() {
        return new ProcessingState(clock);
    }

    @Bean(name = PollerMetadata.DEFAULT_POLLER)
    public PollerMetadata poller(@Value("${polling.delay}") final int pollingDelay) {
        return Pollers.fixedDelay(pollingDelay).get();
    }

    @Bean
    public MessageFileMonitorInitializer messageFileMonitorInitializer() {
        return new MessageFileMonitorInitializer(flowContext, inputReadersLifecycle(), processingState(), aviationProductsHolder, clock, processingChannel,
                successChannel, failChannel, finishChannel, errorMessageChannel, archiveRetryAdvice, failRetryAdvice, exceptionTrapAdvice, filterQueueSize);
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
        final ProcessingState processingState = processingState();
        return IntegrationFlows.from(finishChannel)//
                .handle(ServiceActivators.peekHeader(FileMetadata.class, MessageFileMonitorInitializer.FILE_METADATA, processingState::finish))//
                .nullChannel();
    }
}
