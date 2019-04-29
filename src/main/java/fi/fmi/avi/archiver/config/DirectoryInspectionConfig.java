package fi.fmi.avi.archiver.config;

import java.util.Set;
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

import fi.fmi.avi.archiver.initializing.FileInspectionInitializer;
import fi.fmi.avi.archiver.initializing.SourceDirectoryInitializer;

@Configuration
@EnableIntegration
public class DirectoryInspectionConfig {

    @Autowired
    private IntegrationFlowContext flowContext;

    @Autowired
    private FileTypeHolder fileTypeHolder;

    @Autowired
    private MessageChannel inputChannel;

    @Autowired
    private MessageChannel processingChannel;

    @Autowired
    private MessageChannel outputChannel;

    @Autowired
    private MessageChannel errorChannel;

    @Value("${polling.delay}")
    private int pollingDelay;

    @Bean
    Consumer<SourcePollingChannelAdapterSpec> poller() {
        return c -> c.poller(Pollers.fixedDelay(pollingDelay));
    }

    @Bean(destroyMethod = "dispose")
    public SourceDirectoryInitializer sourceDirectoryInitializer(@Value("${dirs.sourcedirs}") final Set<String> sourceDirs) {
        return new SourceDirectoryInitializer(inputChannel, flowContext, sourceDirs, poller());
    }

    @Bean(destroyMethod = "dispose")
    public FileInspectionInitializer fileInspectorInitializer() {
        return new FileInspectionInitializer(flowContext, fileTypeHolder, inputChannel, processingChannel);
    }

    @Bean
    public IntegrationFlow fileProcessor() {
        // TODO: Add file processing logic
        return IntegrationFlows.from(processingChannel)//
                .channel(outputChannel)//
                .get();
    }

}
