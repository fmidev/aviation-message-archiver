package fi.fmi.avi.archiver.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.file.transformer.FileToStringTransformer;
import org.springframework.messaging.MessageChannel;

@Configuration
public class FileHandlerConfig {

    @Value("${file-handler.charset}")
    private String charset;

    @Autowired
    private MessageChannel processingChannel;

    @Autowired
    private MessageChannel parserChannel;

    @Bean
    public FileToStringTransformer fileToStringTransformer() {
        final FileToStringTransformer transformer = new FileToStringTransformer();
        transformer.setCharset(charset);
        return transformer;
    }

    @Bean
    public IntegrationFlow fileFlow() {
        return IntegrationFlows.from(processingChannel)//
                .transform(fileToStringTransformer())//
                .channel(parserChannel)//
                .get();
    }

}
