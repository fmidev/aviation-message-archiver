package fi.fmi.avi.archiver.config;

import java.io.File;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.file.FileWritingMessageHandler;
import org.springframework.integration.file.support.FileExistsMode;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

@Configuration
@EnableIntegration
public class FileMovingConfig {

    @Value("${dirs.destination}")
    private String destDir;

    @Value("${dirs.error}")
    private String errorDir;

    @Autowired
    private MessageChannel archivedChannel;

    @Autowired
    private MessageChannel failedChannel;

    @Bean
    public MessageHandler archivedDirectory() {
        final FileWritingMessageHandler handler = new FileWritingMessageHandler(new File(destDir));
        handler.setFileExistsMode(FileExistsMode.REPLACE_IF_MODIFIED);
        handler.setExpectReply(false);
        return handler;
    }

    @Bean
    public MessageHandler failedDirectory() {
        final FileWritingMessageHandler handler = new FileWritingMessageHandler(new File(errorDir));
        handler.setFileExistsMode(FileExistsMode.REPLACE_IF_MODIFIED);
        handler.setExpectReply(false);
        return handler;
    }

    @Bean
    public IntegrationFlow archivedFileMover() {
        return IntegrationFlows.from(archivedChannel)//
                .handle(archivedDirectory())//
                .get();
    }

    @Bean
    public IntegrationFlow failedFileMover() {
        return IntegrationFlows.from(failedChannel)//
                .handle(failedDirectory())//
                .get();
    }
}
