package fi.fmi.avi.archiver.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.file.transformer.FileToStringTransformer;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;

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

    @Bean(name = PollerMetadata.DEFAULT_POLLER)
    public PollerMetadata poller(@Value("${polling.delay}") final int pollingDelay) {
        return Pollers.fixedDelay(pollingDelay).get();
    }

    @Bean(destroyMethod = "dispose")
    public MessageFileMonitorInitializer messageFileMonitorInitializer() {
        return new MessageFileMonitorInitializer(flowContext, aviationProductsHolder, processingChannel, archivedChannel, failedChannel);
    }

    @Bean
    public IntegrationFlow fileProcessor() {
        return IntegrationFlows.from(processingChannel)//
                .transform(new FileToStringTransformer()).channel(archivedChannel)//
                .get();
    }

    @Bean
    public IntegrationFlow archiveProcessor() {
        return IntegrationFlows.from(archivedChannel).handle(this).log("Archive").get();
    }

    @ServiceActivator
    public String handle(String payload, MessageHeaders headers) {

        //TODO: Use file name and content
        FileContent.builder()//
                .setMessageHeader(headers)//
                .setContent(payload)
                .build().getHeader();

        return payload;
    }

}
