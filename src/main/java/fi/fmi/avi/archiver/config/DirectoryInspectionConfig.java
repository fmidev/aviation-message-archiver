package fi.fmi.avi.archiver.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.FileCopyUtils;

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
                .transform(this::fileToStringTransformer)
                .channel(archivedChannel)//
                .get();
    }

    private String fileToStringTransformer(final File file) {
        try {
            Reader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), Charset.defaultCharset()));
            return FileCopyUtils.copyToString(reader);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Bean
    public IntegrationFlow archiveProcessor() {
        return IntegrationFlows.from(archivedChannel)
                //Only testing
                .transform(String.class, str -> {System.out.println(str);return str.toUpperCase();})
                .log("Archive")
                .get();
    }


}
