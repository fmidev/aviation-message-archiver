package fi.fmi.avi.archiver.initializing;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.core.GenericSelector;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.file.FileReadingMessageSource;
import org.springframework.integration.file.FileWritingMessageHandler;
import org.springframework.integration.file.filters.RegexPatternFileListFilter;
import org.springframework.integration.file.support.FileExistsMode;
import org.springframework.integration.handler.LoggingHandler.Level;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

public class MessageFileMonitorInitializer {

    private static final String PRODUCT_KEY = "product";
    private static final String INPUT_CATEGORY = "input";
    /**
     * Initializes Message file source directory reading, filename filtering and archiving of the files
     */

    private final IntegrationFlowContext context;

    private final Set<IntegrationFlowContext.IntegrationFlowRegistration> registerations;

    private final AviationProductsHolder aviationProductsHolder;
    private final MessageChannel processingChannel;
    private final MessageChannel archivedChannel;
    private final MessageChannel failedChannel;

    public MessageFileMonitorInitializer(final IntegrationFlowContext context, final AviationProductsHolder aviationProductsHolder,
            final MessageChannel processingChannel, final MessageChannel archivedChannel, final MessageChannel failedChannel) {
        this.context = context;
        this.registerations = new HashSet<>();
        this.aviationProductsHolder = aviationProductsHolder;
        this.processingChannel = processingChannel;
        this.archivedChannel = archivedChannel;
        this.failedChannel = failedChannel;
    }

    @PostConstruct
    private void initializeFilePatternFlows() {

        aviationProductsHolder.getProducts().forEach(product -> {
            final FileReadingMessageSource sourceDirectory = new FileReadingMessageSource();
            sourceDirectory.setDirectory(product.getInputDir());

            final FileWritingMessageHandler archivedDirectory = new FileWritingMessageHandler(product.getArchivedDir());
            archivedDirectory.setFileExistsMode(FileExistsMode.REPLACE_IF_MODIFIED);
            archivedDirectory.setExpectReply(false);

            final FileWritingMessageHandler failedDirectory = new FileWritingMessageHandler(product.getFailedDir());
            archivedDirectory.setFileExistsMode(FileExistsMode.REPLACE_IF_MODIFIED);
            archivedDirectory.setExpectReply(false);

            // Separate input channel needed in order to use multiple different
            // filters for the same source directory
            final PublishSubscribeChannel inputChannel = new PublishSubscribeChannel();

            // Initialize source directory polling. Uses poller bean
            registerations.add(context.registration(IntegrationFlows.from(sourceDirectory)//
                    .channel(inputChannel)//
                    .get()//
            ).register());

            // Integration flow for file name filtering
            product.getFiles().stream().map(fileConfig -> context.registration(IntegrationFlows.from(inputChannel)//
                            .filter(new RegexPatternFileListFilter(fileConfig.getPattern()))//
                            .enrichHeaders(s -> s.header(PRODUCT_KEY, product))//
                            .log(Level.INFO, INPUT_CATEGORY)//
                            .channel(processingChannel)//
                            .get()//
                    ).register()//

            ).forEach(registerations::add);

            // Initialize file moving flows
            final GenericSelector<Message> productFilter = m -> Objects.equals(m.getHeaders().get(PRODUCT_KEY), product);

            registerations.add(context.registration(IntegrationFlows.from(archivedChannel)//
                    .filter(Message.class, productFilter)//
                    .handle(archivedDirectory)//
                    .get()//
            ).register());

            registerations.add(context.registration(IntegrationFlows.from(failedChannel)//
                    .filter(Message.class, productFilter)//
                    .handle(failedDirectory)//
                    .get()//
            ).register());
        });
    }

    public void dispose() {
        registerations.forEach(registeration -> context.remove(registeration.getId()));
    }

}
