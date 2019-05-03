package fi.fmi.avi.archiver.config;

import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.dsl.SourcePollingChannelAdapterSpec;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.messaging.MessageChannel;

import fi.fmi.avi.archiver.initializing.AviationProductsHolder;
import fi.fmi.avi.archiver.initializing.MessageFileMonitorInitializer;

@Configuration
@EnableIntegration
public class DirectoryInspectionConfig {

    @Autowired
    private IntegrationFlowContext flowContext;

    @Autowired
    private AviationProductsHolder aviationProductsHolder;

    @Autowired
    private MessageChannel processingChannel;

    @Autowired
    private MessageChannel archivedChannel;

    @Autowired
    private MessageChannel failedChannel;

    @Value("${polling.delay}")
    private int pollingDelay;

    @Bean
    public Consumer<SourcePollingChannelAdapterSpec> poller() {
        return c -> c.poller(Pollers.fixedDelay(pollingDelay));
    }

    @Bean(destroyMethod = "dispose")
    public MessageFileMonitorInitializer messageFileMonitorInitializer() {
        return new MessageFileMonitorInitializer(flowContext, aviationProductsHolder, processingChannel, archivedChannel, failedChannel, poller());
    }

    @Bean
    public IntegrationFlow fileProcessor() {
        // TODO: Add file processing logic
        return IntegrationFlows.from(processingChannel)//
                .channel(archivedChannel)//
                .get();
    }

}
