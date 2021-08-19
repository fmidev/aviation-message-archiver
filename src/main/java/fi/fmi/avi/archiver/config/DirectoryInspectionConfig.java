package fi.fmi.avi.archiver.config;

import fi.fmi.avi.archiver.initializing.AviationProductsHolder;
import fi.fmi.avi.archiver.initializing.MessageFileMonitorInitializer;
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

@Configuration
public class DirectoryInspectionConfig {

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

    @Bean(name = PollerMetadata.DEFAULT_POLLER)
    public PollerMetadata poller(@Value("${polling.delay}") final int pollingDelay) {
        return Pollers.fixedDelay(pollingDelay).get();
    }

    @Bean(destroyMethod = "dispose")
    public MessageFileMonitorInitializer messageFileMonitorInitializer() {
        return new MessageFileMonitorInitializer(flowContext, aviationProductsHolder, processingChannel, successChannel, failChannel, errorMessageChannel);
    }

    @Bean
    public IntegrationFlow archiveFlow() {
        return IntegrationFlows.from(successChannel).log("Archive").get();
    }

    @Bean
    public IntegrationFlow archiveRouter() {
        return IntegrationFlows.from(archiveChannel)
                .route("headers." + MessageFileMonitorInitializer.FAILED_MESSAGES + ".isEmpty()" +
                        " and !headers." + MessageFileMonitorInitializer.FILE_PARSED_PARTIALLY, r -> r//
                        .channelMapping(true, successChannel)
                        .channelMapping(false, failChannel)
                ).get();
    }

}
