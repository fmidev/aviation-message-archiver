package fi.fmi.avi.archiver.config;

import com.google.common.collect.ImmutableList;
import fi.fmi.avi.archiver.ProcessingState;
import fi.fmi.avi.archiver.config.model.AviationProduct;
import fi.fmi.avi.archiver.config.model.FileConfig;
import fi.fmi.avi.archiver.database.DatabaseService;
import fi.fmi.avi.archiver.file.FileMetadata;
import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.populator.MessagePopulatorService;
import fi.fmi.avi.archiver.spring.context.CompoundLifecycle;
import fi.fmi.avi.archiver.spring.integration.dsl.ServiceActivators;
import fi.fmi.avi.archiver.spring.integration.file.filters.AcceptUnchangedFileListFilter;
import fi.fmi.avi.archiver.spring.integration.file.filters.ProcessingFileListFilter;
import fi.fmi.avi.archiver.spring.retry.RetryAdviceFactory;
import org.aopalliance.aop.Advice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.core.GenericSelector;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.file.FileNameGenerator;
import org.springframework.integration.file.FileReadingMessageSource;
import org.springframework.integration.file.FileWritingMessageHandler;
import org.springframework.integration.file.filters.AcceptOnceFileListFilter;
import org.springframework.integration.file.filters.ChainFileListFilter;
import org.springframework.integration.file.filters.RegexPatternFileListFilter;
import org.springframework.integration.file.support.FileExistsMode;
import org.springframework.integration.file.transformer.FileToStringTransformer;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.integration.handler.advice.ExpressionEvaluatingRequestHandlerAdvice;
import org.springframework.integration.handler.advice.RequestHandlerRetryAdvice;
import org.springframework.integration.transformer.GenericTransformer;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static org.springframework.integration.file.FileHeaders.FILENAME;

@Configuration
public class IntegrationFlowConfig {

    public static final String FILE_METADATA = "file_metadata";
    public static final String FAILED_MESSAGES = "processing_failures";
    public static final String DISCARDED_MESSAGES = "processing_discards";
    public static final String FILE_PARSE_ERRORS = "file_parsed_partially";

    private static final Logger LOGGER = LoggerFactory.getLogger(IntegrationFlowConfig.class);

    @Nullable
    private static Message<?> errorMessageToOriginalMessage(final ErrorMessage errorMessage) {
        final Message<?> failedMessage;
        if (errorMessage.getPayload() instanceof MessagingException) {
            failedMessage = ((MessagingException) errorMessage.getPayload()).getFailedMessage();
        }
        // Attempt to use original message if the exception is not a MessagingException
        else if (errorMessage.getOriginalMessage() != null) {
            failedMessage = errorMessage.getOriginalMessage();
        } else {
            // Unable to get the original message, log the exception
            LOGGER.error("Unable to extract message from error message", errorMessage.getPayload());
            return null;
        }
        LOGGER.error("Processing message {} failed: ", failedMessage, errorMessage.getPayload());
        return failedMessage;
    }

    @Bean
    IntegrationFlow archivalFlow(final FileToStringTransformer fileToStringTransformer, final RequestHandlerRetryAdvice fileReadingRetryAdvice,
                                 final ParserConfig.FileParserService fileParserService, final MessagePopulatorService messagePopulatorService,
                                 final DatabaseService databaseService, final MessageChannel processingChannel, final MessageChannel parserChannel,
                                 final MessageChannel populatorChannel, final MessageChannel databaseChannel, final MessageChannel archiveChannel,
                                 final MessageChannel successChannel, final MessageChannel failChannel) {
        return IntegrationFlows.from(processingChannel)
                .transform(Message.class, fileToStringTransformer::transform, spec -> spec.advice(fileReadingRetryAdvice))
                .channel(parserChannel)
                .<String>filter(content -> content != null && !content.isEmpty(), discards -> discards.discardChannel(failChannel))
                .handle(fileParserService::parse)
                .<List<InputAviationMessage>>filter(messages -> !messages.isEmpty(), discards -> discards.discardChannel(failChannel))
                .channel(populatorChannel)
                .handle(messagePopulatorService::populateMessages)
                .channel(databaseChannel)
                .<List<ArchiveAviationMessage>>handle((payload, headers) -> databaseService.insertMessages(payload))
                .channel(archiveChannel)
                .route("headers." + FAILED_MESSAGES + ".isEmpty()" //
                        + " and !headers." + FILE_PARSE_ERRORS, r -> r//
                        .channelMapping(true, successChannel)//
                        .channelMapping(false, failChannel))//
                .get();
    }

    @Bean
    IntegrationFlow logSuccessFlow(final MessageChannel successChannel) {
        return IntegrationFlows.from(successChannel).log("Archive").get();
    }

    @Bean
    IntegrationFlow finishFlow(final ProcessingState processingState, final MessageChannel finishChannel) {
        return IntegrationFlows.from(finishChannel)//
                .handle(ServiceActivators.peekHeader(FileMetadata.class, FILE_METADATA, processingState::finish))//
                .nullChannel();
    }

    @Bean
    IntegrationFlow errorMessageFlow(final MessageChannel errorMessageChannel, final MessageChannel failChannel) {
        return IntegrationFlows.from(errorMessageChannel)//
                .handle(ErrorMessage.class, (payload, headers) -> errorMessageToOriginalMessage(payload))
                .transform(Message.class, m -> m.getHeaders().get(FileHeaders.ORIGINAL_FILE, File.class))//
                .channel(failChannel)//
                .get();
    }

    @Bean
    IntegrationFlow errorLoggingFlow(final MessageChannel errorLoggingChannel, final MessageChannel finishChannel) {
        return IntegrationFlows.from(errorLoggingChannel)//
                .handle(ErrorMessage.class, (payload, headers) -> errorMessageToOriginalMessage(payload)).channel(finishChannel)//
                .get();
    }

    @Bean
    FileToStringTransformer fileToStringTransformer(@Value("${file-handler.charset}") final String charset) {
        final FileToStringTransformer transformer = new FileToStringTransformer();
        transformer.setCharset(charset);
        return transformer;
    }

    @Bean
    FileNameGenerator timestampAppender(final Clock clock) {
        return msg -> msg.getHeaders().get(FILENAME) + "." + clock.millis();
    }

    // Trap exceptions to avoid infinite looping when the error message flow itself results in exceptions
    @Bean
    Advice exceptionTrapAdvice(final MessageChannel errorLoggingChannel) {
        final ExpressionEvaluatingRequestHandlerAdvice advice = new ExpressionEvaluatingRequestHandlerAdvice();
        advice.setFailureChannel(errorLoggingChannel);
        advice.setTrapException(true);
        return advice;
    }

    @Bean
    RetryAdviceFactory retryAdviceFactory(//
                                          @Value("${file-handler.retry.initial-interval}") final Duration initialInterval, //
                                          @Value("${file-handler.retry.max-interval}") final Duration maxInterval, //
                                          @Value("${file-handler.retry.multiplier}") final int retryMultiplier, //
                                          @Value("${file-handler.retry.timeout}") final Duration timeout) {
        return new RetryAdviceFactory(initialInterval, maxInterval, retryMultiplier, timeout);
    }

    @Bean
    RequestHandlerRetryAdvice fileReadingRetryAdvice(final RetryAdviceFactory retryAdviceFactory) {
        return retryAdviceFactory.create("File reading");
    }

    @Bean
    RequestHandlerRetryAdvice archiveRetryAdvice(final RetryAdviceFactory retryAdviceFactory) {
        return retryAdviceFactory.create("Writing to archive dir");
    }

    @Bean
    RequestHandlerRetryAdvice failRetryAdvice(final RetryAdviceFactory retryAdviceFactory) {
        return retryAdviceFactory.create("Writing to fail dir");
    }

    @Bean
    List<Advice> archiveAdviceChain(final Advice exceptionTrapAdvice, final RequestHandlerRetryAdvice archiveRetryAdvice) {
        return ImmutableList.of(exceptionTrapAdvice, archiveRetryAdvice);
    }

    @Bean
    List<Advice> failAdviceChain(final Advice exceptionTrapAdvice, final RequestHandlerRetryAdvice failRetryAdvice) {
        return ImmutableList.of(exceptionTrapAdvice, failRetryAdvice);
    }

    @Bean
    @SuppressWarnings("rawtypes")
    GenericTransformer<Message, File> headerToFileTransformer() {
        return message -> message.getHeaders().get(FileHeaders.ORIGINAL_FILE, File.class);
    }

    @Component
    static class ProductFlowsInitializer {
        private static final String PRODUCT_KEY = "product";
        private static final String INPUT_CATEGORY = "input";

        private final Set<IntegrationFlowContext.IntegrationFlowRegistration> registrations = new HashSet<>();

        private final IntegrationFlowContext context;
        private final Map<String, AviationProduct> aviationProducts;
        private final CompoundLifecycle inputReadersLifecycle;
        private final ProcessingState processingState;
        private final List<Advice> archiveAdviceChain;
        private final List<Advice> failAdviceChain;
        private final FileNameGenerator timestampAppender;
        @SuppressWarnings("rawtypes")
        private final GenericTransformer<Message, File> headerToFileTransformer;
        private final int filterQueueSize;
        private final MessageChannel processingChannel;
        private final MessageChannel errorMessageChannel;
        private final MessageChannel successChannel;
        private final MessageChannel failChannel;
        private final MessageChannel finishChannel;

        ProductFlowsInitializer(final IntegrationFlowContext context, final Map<String, AviationProduct> aviationProducts,
                                final CompoundLifecycle inputReadersLifecycle, final ProcessingState processingState, final List<Advice> archiveAdviceChain,
                                final List<Advice> failAdviceChain, final FileNameGenerator timestampAppender,
                                @SuppressWarnings("rawtypes") final GenericTransformer<Message, File> headerToFileTransformer,
                                @Value("${polling.filter-queue-size}") final int filterQueueSize, final MessageChannel processingChannel,
                                final MessageChannel errorMessageChannel, final MessageChannel successChannel, final MessageChannel failChannel,
                                final MessageChannel finishChannel) {
            this.context = requireNonNull(context, "context");
            this.aviationProducts = requireNonNull(aviationProducts, "aviationProducts");
            this.inputReadersLifecycle = requireNonNull(inputReadersLifecycle, "inputReadersLifecycle");
            this.processingState = requireNonNull(processingState, "processingState");
            this.archiveAdviceChain = requireNonNull(archiveAdviceChain, "archiveAdviceChain");
            this.failAdviceChain = requireNonNull(failAdviceChain, "failAdviceChain");
            this.timestampAppender = requireNonNull(timestampAppender, "timestampAppender");
            this.headerToFileTransformer = requireNonNull(headerToFileTransformer, "headerToFileTransformer");
            this.filterQueueSize = filterQueueSize;
            this.processingChannel = requireNonNull(processingChannel, "processingChannel");
            this.errorMessageChannel = requireNonNull(errorMessageChannel, "errorMessageChannel");
            this.successChannel = requireNonNull(successChannel, "successChannel");
            this.failChannel = requireNonNull(failChannel, "failChannel");
            this.finishChannel = requireNonNull(finishChannel, "finishChannel");
        }

        private static FileMetadata createFileMetadata(final Message<?> message, final FileConfig fileConfig, final String productIdentifier) {
            final String filename = requireNonNull(message.getHeaders().get(FILENAME, String.class), FILENAME);
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
        void initializeProductFlows() {
            aviationProducts.values().forEach(product -> {
                final FileReadingMessageSource sourceReader = createMessageSource(product.getInputDir(), product.getId());
                inputReadersLifecycle.add(sourceReader);

                // Separate input channel needed in order to use multiple different
                // filters for the same source directory
                final PublishSubscribeChannel inputChannel = new PublishSubscribeChannel();

                // Initialize source directory polling. Uses poller bean
                registerIntegrationFlow(IntegrationFlows.from(sourceReader)//
                        .channel(inputChannel)//
                        .get());

                // Integration flow for file name filtering
                registerAllIntegrationFlows(product.getFileConfigs().stream()//
                        .map(fileConfig -> IntegrationFlows.from(inputChannel)//
                                .filter(new RegexPatternFileListFilter(fileConfig.getPattern())::accept)//
                                .enrichHeaders(s -> s.header(PRODUCT_KEY, product)//
                                        .headerFunction(MessageHeaders.ERROR_CHANNEL, message -> errorMessageChannel)
                                        .headerFunction(FILE_METADATA, message -> createFileMetadata(message, fileConfig, product.getId())))
                                .handle(ServiceActivators.peekHeader(FileMetadata.class, FILE_METADATA, processingState::start))//
                                .log(LoggingHandler.Level.INFO, INPUT_CATEGORY)//
                                .channel(processingChannel)//
                                .get()//
                        ));

                @SuppressWarnings("rawtypes") final GenericSelector<Message> productFilter = m -> Objects.equals(m.getHeaders().get(PRODUCT_KEY), product);

                registerIntegrationFlow(IntegrationFlows.from(successChannel)//
                        .filter(Message.class, productFilter)//
                        .transform(Message.class, headerToFileTransformer)//
                        .handle(createArchiveHandler(product.getArchiveDir()))//
                        .channel(finishChannel)//
                        .get());

                registerIntegrationFlow(IntegrationFlows.from(failChannel)//
                        .filter(Message.class, productFilter)//
                        .transform(Message.class, headerToFileTransformer)//
                        .handle(createFailHandler(product.getFailDir()))//
                        .channel(finishChannel)//
                        .get());
            });
        }

        @PreDestroy
        void dispose() {
            registrations.forEach(registration -> context.remove(registration.getId()));
        }

        private void registerIntegrationFlow(final IntegrationFlow integrationFlow) {
            registrations.add(context.registration(integrationFlow)//
                    .autoStartup(false)//
                    .register());
        }

        private void registerAllIntegrationFlows(final Stream<IntegrationFlow> integrationFlows) {
            integrationFlows.forEach(this::registerIntegrationFlow);
        }

        private FileReadingMessageSource createMessageSource(final File inputDir, final String productId) {
            final FileReadingMessageSource sourceDirectory = new FileReadingMessageSource();
            sourceDirectory.setDirectory(inputDir);
            sourceDirectory.setFilter(createSourceFileListFilter(productId));
            return sourceDirectory;
        }

        private ChainFileListFilter<File> createSourceFileListFilter(final String productId) {
            return new ChainFileListFilter<>(ImmutableList.of(//
                    new ProcessingFileListFilter(processingState, productId), //
                    new AcceptUnchangedFileListFilter(),//
                    new AcceptOnceFileListFilter<>(filterQueueSize)));
        }

        private FileWritingMessageHandler createArchiveHandler(final File destinationDir) {
            final FileWritingMessageHandler archiveHandler = new FileWritingMessageHandler(destinationDir);
            archiveHandler.setFileExistsMode(FileExistsMode.REPLACE_IF_MODIFIED);
            archiveHandler.setDeleteSourceFiles(true);
            archiveHandler.setAdviceChain(archiveAdviceChain);
            archiveHandler.setFileNameGenerator(timestampAppender);
            return archiveHandler;
        }

        private FileWritingMessageHandler createFailHandler(final File destinationDir) {
            final FileWritingMessageHandler failHandler = new FileWritingMessageHandler(destinationDir);
            failHandler.setFileExistsMode(FileExistsMode.REPLACE_IF_MODIFIED);
            failHandler.setDeleteSourceFiles(true);
            failHandler.setAdviceChain(failAdviceChain);
            failHandler.setFileNameGenerator(timestampAppender);
            return failHandler;
        }
    }

}
