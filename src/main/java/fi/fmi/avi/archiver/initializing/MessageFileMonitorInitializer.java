package fi.fmi.avi.archiver.initializing;

import fi.fmi.avi.archiver.file.FileConfig;
import fi.fmi.avi.archiver.file.FileMetadata;
import fi.fmi.avi.archiver.file.FilenamePattern;
import fi.fmi.avi.archiver.transformer.HeaderToFileTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.core.GenericSelector;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.file.FileReadingMessageSource;
import org.springframework.integration.file.FileWritingMessageHandler;
import org.springframework.integration.file.filters.RegexPatternFileListFilter;
import org.springframework.integration.file.support.FileExistsMode;
import org.springframework.integration.handler.LoggingHandler.Level;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * Initializes Message file source directory reading, filename filtering and archiving of the files.
 */
public class MessageFileMonitorInitializer {
    public static final String FILE_METADATA = "file_metadata";
    public static final String FAILED_MESSAGES = "processing_failures";
    public static final String FILE_PARSE_ERRORS = "file_parsed_partially";

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageFileMonitorInitializer.class);

    private static final String PRODUCT_KEY = "product";
    private static final String INPUT_CATEGORY = "input";

    private final IntegrationFlowContext context;
    private final Set<IntegrationFlowContext.IntegrationFlowRegistration> registerations;

    private final AviationProductsHolder aviationProductsHolder;
    private final MessageChannel processingChannel;
    private final MessageChannel successChannel;
    private final MessageChannel failChannel;
    private final MessageChannel errorMessageChannel;

    public MessageFileMonitorInitializer(final IntegrationFlowContext context, final AviationProductsHolder aviationProductsHolder,
                                         final MessageChannel processingChannel, final MessageChannel successChannel, final MessageChannel failChannel,
                                         final MessageChannel errorMessageChannel) {
        this.context = requireNonNull(context, "context");
        this.registerations = new HashSet<>();
        this.aviationProductsHolder = requireNonNull(aviationProductsHolder, "aviationProductsHolder");
        this.processingChannel = requireNonNull(processingChannel, "processingChannel");
        this.successChannel = requireNonNull(successChannel, "archiveChannel");
        this.failChannel = requireNonNull(failChannel, "failChannel");
        this.errorMessageChannel = requireNonNull(errorMessageChannel, "errorMessageChannel");
    }

    @PostConstruct
    private void initializeFilePatternFlows() {
        aviationProductsHolder.getProducts().values().forEach(product -> {
            final FileReadingMessageSource sourceDirectory = new FileReadingMessageSource();
            sourceDirectory.setDirectory(product.getInputDir());

            final FileWritingMessageHandler archiveDirectory = new FileWritingMessageHandler(product.getArchiveDir());
            archiveDirectory.setFileExistsMode(FileExistsMode.REPLACE_IF_MODIFIED);
            archiveDirectory.setExpectReply(false);

            final FileWritingMessageHandler failDirectory = new FileWritingMessageHandler(product.getFailDir());
            archiveDirectory.setFileExistsMode(FileExistsMode.REPLACE_IF_MODIFIED);
            archiveDirectory.setExpectReply(false);

            // Separate input channel needed in order to use multiple different
            // filters for the same source directory
            final PublishSubscribeChannel inputChannel = new PublishSubscribeChannel();

            // Initialize source directory polling. Uses poller bean
            registerations.add(context.registration(IntegrationFlows.from(sourceDirectory)//
                    .channel(inputChannel)//
                    .get()//
            ).register());

            // Integration flow for file name filtering
            product.getFileConfigs().stream().map(fileConfig -> context.registration(IntegrationFlows.from(inputChannel)//
                            .filter(new RegexPatternFileListFilter(fileConfig.getPattern())::accept)//
                            .enrichHeaders(s -> s.header(PRODUCT_KEY, product)//
                                    .headerFunction(MessageHeaders.ERROR_CHANNEL, message -> errorMessageChannel)
                                    .headerFunction(FILE_METADATA, message -> createFileMetadata(message, fileConfig, product.getId())))
                            .log(Level.INFO, INPUT_CATEGORY)//
                            .channel(processingChannel)//
                            .get()//
                    ).register()//
            ).forEach(registerations::add);

            // Initialize file moving flows
            final GenericSelector<Message> productFilter = m -> Objects.equals(m.getHeaders().get(PRODUCT_KEY), product);
            final HeaderToFileTransformer headerToFileTransformer = new HeaderToFileTransformer();

            registerations.add(context.registration(IntegrationFlows.from(successChannel)//
                    .filter(Message.class, productFilter)//
                    .transform(headerToFileTransformer)//
                    .handle(archiveDirectory)//
                    .get()//
            ).register());

            registerations.add(context.registration(IntegrationFlows.from(failChannel)//
                    .filter(Message.class, productFilter)//
                    .transform(headerToFileTransformer)//
                    .handle(failDirectory)//
                    .nullChannel()//
            ).register());
        });
    }

    public void dispose() {
        registerations.forEach(registration -> context.remove(registration.getId()));
    }

    private static FileMetadata createFileMetadata(final Message<?> message, final FileConfig fileConfig,
                                                   final String productIdentifier) {
        final String filename = message.getHeaders().get(FileHeaders.FILENAME, String.class);
        return FileMetadata.builder()
                .setFilename(filename)
                .setFileConfig(fileConfig)
                .setFilenamePattern(new FilenamePattern(filename, fileConfig.getPattern()))
                .setProductIdentifier(productIdentifier)
                .setFileModified(getFileModified(message))
                .build();
    }

    private static Optional<Instant> getFileModified(final Message<?> message) {
        final File file = message.getHeaders().get(FileHeaders.ORIGINAL_FILE, File.class);
        if (file != null) {
            try {
                return Optional.of(Files.getLastModifiedTime(file.toPath()).toInstant());
            } catch (final IOException e) {
                LOGGER.error("Unable to get file last modified time: {}", file.getName(), e);
            }
        }
        return Optional.empty();
    }

}
