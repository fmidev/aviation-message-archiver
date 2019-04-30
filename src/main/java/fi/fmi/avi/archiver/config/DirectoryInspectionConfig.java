package fi.fmi.avi.archiver.config;

import java.io.File;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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

import fi.fmi.avi.archiver.initializing.AviFileTypeHolder;
import fi.fmi.avi.archiver.initializing.InputDirectoryInitializer;
import fi.fmi.avi.archiver.initializing.MessageFileMonitorInitializer;

@Configuration
@EnableIntegration
public class DirectoryInspectionConfig {

    @Autowired
    private IntegrationFlowContext flowContext;

    @Autowired
    private AviFileTypeHolder aviFileTypeHolder;

    @Autowired
    private MessageChannel inputChannel;

    @Autowired
    private MessageChannel processingChannel;

    @Autowired
    private MessageChannel archivedChannel;

    @Value("${polling.delay}")
    private int pollingDelay;

    @Value("${dirs.sourcedirs}")
    private Set<String> sourceDirs;

    @Bean
    public Consumer<SourcePollingChannelAdapterSpec> poller() {
        return c -> c.poller(Pollers.fixedDelay(pollingDelay));
    }

    @Bean
    public Set<File> sourceDirs() {
        return sourceDirs.stream().map(File::new).collect(Collectors.toSet());
    }

    @Bean(destroyMethod = "dispose")
    public InputDirectoryInitializer inputDirectoryInitializer() {
        return new InputDirectoryInitializer(inputChannel, flowContext, sourceDirs(), poller());
    }

    @Bean(destroyMethod = "dispose")
    public MessageFileMonitorInitializer messageFileMonitorInitializer() {
        return new MessageFileMonitorInitializer(flowContext, aviFileTypeHolder, inputChannel, processingChannel);
    }

    @Bean
    public IntegrationFlow fileProcessor() {
        // TODO: Add file processing logic
        return IntegrationFlows.from(processingChannel)//
                .channel(archivedChannel)//
                .get();
    }

}
