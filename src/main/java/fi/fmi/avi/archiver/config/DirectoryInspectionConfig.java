package fi.fmi.avi.archiver.config;

import java.io.File;
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
import org.springframework.integration.file.FileWritingMessageHandler;
import org.springframework.integration.file.filters.RegexPatternFileListFilter;
import org.springframework.integration.file.support.FileExistsMode;
import org.springframework.messaging.MessageHandler;

@Configuration
@EnableIntegration
public class DirectoryInspectionConfig {

    public static final String INPUT_CHANNEL = "input";
    public static final String MAIN_CHANNEL = "main";
    public static final String ERROR_CHANNEL = "error";

    @Autowired
    private IntegrationFlowContext flowContext;

    @Value("${dirs.destination}")
    private String destDir;

    @Value("${dirs.error}")
    private String errorDir;

    @Value("${polling.delay}")
    private int pollingDelay;

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
        return new SourceDirectoryInitializer(INPUT_CHANNEL, flowContext, sourceDirs, poller());
    }

    @Bean
    public IntegrationFlow fileInspector(@Value("${file.pattern}") final String filePattern) {
        // TODO: Add file handling logic
        return IntegrationFlows.from(INPUT_CHANNEL)//
                .filter(new RegexPatternFileListFilter(filePattern))//
                .channel(MAIN_CHANNEL)//
                .get();
    }

    @Bean
    public IntegrationFlow validFileMover() {
        return IntegrationFlows.from(MAIN_CHANNEL)//
                .handle(destinationDirectory())//
                .get();
    }

    @Bean
    public IntegrationFlow invalidFileMover() {
        return IntegrationFlows.from(ERROR_CHANNEL)//
                .handle(errorDirectory())//
                .get();
    }
}
