package fi.fmi.avi.archiver.initializing;

import com.google.common.collect.ImmutableList;
import fi.fmi.avi.archiver.ProcessingMetrics;
import fi.fmi.avi.archiver.file.FileConfig;
import fi.fmi.avi.archiver.file.FileMetadata;
import fi.fmi.avi.archiver.spring.context.CompoundLifecycle;
import fi.fmi.avi.archiver.spring.integration.dsl.ServiceActivators;
import fi.fmi.avi.archiver.spring.integration.file.filters.AcceptOnceUnchangedFileListFilter;
import fi.fmi.avi.archiver.transformer.HeaderToFileTransformer;
import org.aopalliance.aop.Advice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.core.GenericSelector;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.file.FileNameGenerator;
import org.springframework.integration.file.FileReadingMessageSource;
import org.springframework.integration.file.FileWritingMessageHandler;
import org.springframework.integration.file.filters.RegexPatternFileListFilter;
import org.springframework.integration.file.support.FileExistsMode;
import org.springframework.integration.handler.LoggingHandler.Level;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

/**
 * Initializes Message file source directory reading, filename filtering and archiving of the files.
 */
public class MessageFileMonitorInitializer {
    public static final String FILE_METADATA = "file_metadata";
    public static final String FAILED_MESSAGES = "processing_failures";
    public static final String DISCARDED_MESSAGES = "processing_discards";
    public static final String FILE_PARSE_ERRORS = "file_parsed_partially";

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageFileMonitorInitializer.class);

    private static final String PRODUCT_KEY = "product";
    private static final String INPUT_CATEGORY = "input";

    private final IntegrationFlowContext context;
    private final CompoundLifecycle inputReadersLifecycle;
    private final ProcessingMetrics processingMetrics;

    private final AviationProductsHolder aviationProductsHolder;
    private final Clock clock;
    private final MessageChannel processingChannel;
    private final MessageChannel successChannel;
    private final MessageChannel failChannel;
    private final MessageChannel finishChannel;
    private final MessageChannel errorMessageChannel;
    private final Advice archiveRetryAdvice;
    private final Advice failRetryAdvice;
    private final Advice exceptionTrapAdvice;
    private final int filterQueueSize;

    private final Set<IntegrationFlowContext.IntegrationFlowRegistration> registrations = new HashSet<>();

    public MessageFileMonitorInitializer(final IntegrationFlowContext context, final CompoundLifecycle inputReadersLifecycle,
                                         final ProcessingMetrics processingMetrics, final AviationProductsHolder aviationProductsHolder, final Clock clock,
                                         final MessageChannel processingChannel, final MessageChannel successChannel, final MessageChannel failChannel, final MessageChannel finishChannel,
                                         final MessageChannel errorMessageChannel, final Advice archiveRetryAdvice, final Advice failRetryAdvice, final Advice exceptionTrapAdvice,
                                         final int filterQueueSize) {
        this.context = requireNonNull(context, "context");
        this.inputReadersLifecycle = requireNonNull(inputReadersLifecycle, "inputReadersLifecycle");
        this.processingMetrics = requireNonNull(processingMetrics, "processingMetrics");
        this.aviationProductsHolder = requireNonNull(aviationProductsHolder, "aviationProductsHolder");
        this.clock = requireNonNull(clock, "clock");
        this.processingChannel = requireNonNull(processingChannel, "processingChannel");
        this.successChannel = requireNonNull(successChannel, "successChannel");
        this.failChannel = requireNonNull(failChannel, "failChannel");
        this.finishChannel = requireNonNull(finishChannel, "finishChannel");
        this.errorMessageChannel = requireNonNull(errorMessageChannel, "errorMessageChannel");
        this.archiveRetryAdvice = requireNonNull(archiveRetryAdvice, "archiveRetryAdvice");
        this.failRetryAdvice = requireNonNull(failRetryAdvice, "failRetryAdvice");
        this.exceptionTrapAdvice = requireNonNull(exceptionTrapAdvice, "exceptionTrapAdvice");
        this.filterQueueSize = filterQueueSize;
    }

    private static FileMetadata createFileMetadata(final Message<?> message, final FileConfig fileConfig, final String productIdentifier) {
        final String filename = message.getHeaders().get(FileHeaders.FILENAME, String.class);
        return FileMetadata.builder()
                .setFilename(filename)
                .setFileConfig(fileConfig)
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

    @PostConstruct
    private void initializeFilePatternFlows() {
        final FileNameGenerator timestampAppender = msg -> msg.getHeaders().get(FileHeaders.FILENAME) + "." + clock.millis();
        final List<Advice> archiveAdviceChain = ImmutableList.of(exceptionTrapAdvice, archiveRetryAdvice);
        final List<Advice> failAdviceChain = ImmutableList.of(exceptionTrapAdvice, failRetryAdvice);

        aviationProductsHolder.getProducts().values().forEach(product -> {
            final FileReadingMessageSource sourceDirectory = new FileReadingMessageSource();
            sourceDirectory.setDirectory(product.getInputDir());
            sourceDirectory.setFilter(new AcceptOnceUnchangedFileListFilter(filterQueueSize));
            inputReadersLifecycle.add(sourceDirectory);

            final FileWritingMessageHandler archiveDirectory = new FileWritingMessageHandler(product.getArchiveDir());
            archiveDirectory.setFileExistsMode(FileExistsMode.REPLACE_IF_MODIFIED);
            archiveDirectory.setDeleteSourceFiles(true);
            archiveDirectory.setAdviceChain(archiveAdviceChain);
            archiveDirectory.setFileNameGenerator(timestampAppender);

            final FileWritingMessageHandler failDirectory = new FileWritingMessageHandler(product.getFailDir());
            failDirectory.setFileExistsMode(FileExistsMode.REPLACE_IF_MODIFIED);
            failDirectory.setDeleteSourceFiles(true);
            failDirectory.setAdviceChain(failAdviceChain);
            failDirectory.setFileNameGenerator(timestampAppender);

            // Separate input channel needed in order to use multiple different
            // filters for the same source directory
            final PublishSubscribeChannel inputChannel = new PublishSubscribeChannel();

            // Initialize source directory polling. Uses poller bean
            registerIntegrationFlow(IntegrationFlows.from(sourceDirectory)//
                    .channel(inputChannel)//
                    .get());

            // Integration flow for file name filtering
            registerAllIntegrationFlows(product.getFileConfigs().stream()//
                    .map(fileConfig -> IntegrationFlows.from(inputChannel)//
                            .filter(new RegexPatternFileListFilter(fileConfig.getPattern())::accept)//
                            .enrichHeaders(s -> s.header(PRODUCT_KEY, product)//
                                    .headerFunction(MessageHeaders.ERROR_CHANNEL, message -> errorMessageChannel)
                                    .headerFunction(FILE_METADATA, message -> createFileMetadata(message, fileConfig, product.getId())))
                            .handle(ServiceActivators.peekHeader(FileMetadata.class, FILE_METADATA, processingMetrics::start))//
                            .log(Level.INFO, INPUT_CATEGORY)//
                            .channel(processingChannel)//
                            .get()//
                    ));

            // Initialize file moving flows
            final GenericSelector<Message> productFilter = m -> Objects.equals(m.getHeaders().get(PRODUCT_KEY), product);
            final HeaderToFileTransformer headerToFileTransformer = new HeaderToFileTransformer();

            registerIntegrationFlow(IntegrationFlows.from(successChannel)//
                    .filter(Message.class, productFilter)//
                    .transform(headerToFileTransformer)//
                    .handle(archiveDirectory)//
                    .channel(finishChannel)//
                    .get());

            registerIntegrationFlow(IntegrationFlows.from(failChannel)//
                    .filter(Message.class, productFilter)//
                    .transform(headerToFileTransformer)//
                    .handle(failDirectory)//
                    .channel(finishChannel)//
                    .get());
        });
    }

    private void registerIntegrationFlow(final IntegrationFlow integrationFlow) {
        registrations.add(context.registration(integrationFlow)//
                .autoStartup(false)//
                .register());
    }

    private void registerAllIntegrationFlows(final Stream<IntegrationFlow> integrationFlows) {
        integrationFlows.forEach(this::registerIntegrationFlow);
    }

    @PreDestroy
    public void dispose() {
        registrations.forEach(registration -> context.remove(registration.getId()));
    }

}
