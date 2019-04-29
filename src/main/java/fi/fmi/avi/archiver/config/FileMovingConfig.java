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
    private MessageChannel outputChannel;

    @Autowired
    private MessageChannel errorChannel;

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
    public IntegrationFlow validFileMover() {
        return IntegrationFlows.from(outputChannel)//
                .transform(p -> p.toString()).handle(destinationDirectory())//
                .get();
    }

    @Bean
    public IntegrationFlow invalidFileMover() {
        return IntegrationFlows.from(errorChannel)//
                .handle(errorDirectory())//
                .get();
    }
}
