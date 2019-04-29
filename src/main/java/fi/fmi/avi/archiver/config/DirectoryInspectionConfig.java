package fi.fmi.avi.archiver.config;

import java.io.File;
import java.util.Set;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.dsl.SourcePollingChannelAdapterSpec;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.file.FileWritingMessageHandler;
import org.springframework.integration.file.support.FileExistsMode;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

import fi.fmi.avi.archiver.initializing.FileInspectionInitializer;
import fi.fmi.avi.archiver.initializing.SourceDirectoryInitializer;

@Configuration
@EnableIntegration
public class DirectoryInspectionConfig {

    @Autowired
    private IntegrationFlowContext flowContext;

    @Autowired
    private FileTypeHolder fileTypeHolder;

    @Value("${dirs.destination}")
    private String destDir;

    @Value("${dirs.error}")
    private String errorDir;

    @Value("${polling.delay}")
    private int pollingDelay;

    @Bean
    public MessageChannel inputChannel() {
        return new PublishSubscribeChannel();
    }

    @Bean
    public MessageChannel processingChannel() {
        return new PublishSubscribeChannel();
    }

    @Bean
    public MessageChannel errorChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageHandler destinationDirectory() {
        final FileWritingMessageHandler handler = new FileWritingMessageHandler(new File(destDir));
        handler.setFileExistsMode(FileExistsMode.REPLACE_IF_MODIFIED);
        handler.setExpectReply(false);
        return handler;
    }

    @Bean
    public MessageHandler errorDirectory() {
        final FileWritingMessageHandler handler = new FileWritingMessageHandler(new File(errorDir));
        handler.setFileExistsMode(FileExistsMode.REPLACE_IF_MODIFIED);
        handler.setExpectReply(false);
        return handler;
    }

    @Bean
    Consumer<SourcePollingChannelAdapterSpec> poller() {
        return c -> c.poller(Pollers.fixedDelay(pollingDelay));
    }

    @Bean(destroyMethod = "dispose")
    public SourceDirectoryInitializer sourceDirectoryInitializer(@Value("${dirs.sourcedirs}") final Set<String> sourceDirs) {
        return new SourceDirectoryInitializer(inputChannel(), flowContext, sourceDirs, poller());
    }

    @Bean(destroyMethod = "dispose")
    public FileInspectionInitializer fileInspectorInitializer() {
        return new FileInspectionInitializer(flowContext, fileTypeHolder, inputChannel(), processingChannel());
    }

    @Bean
    public IntegrationFlow validFileMover() {
        return IntegrationFlows.from(processingChannel())//
                .handle(destinationDirectory())//
                .get();
    }

    @Bean
    public IntegrationFlow invalidFileMover() {
        return IntegrationFlows.from(errorChannel())//
                .handle(errorDirectory())//
                .get();
    }

}
