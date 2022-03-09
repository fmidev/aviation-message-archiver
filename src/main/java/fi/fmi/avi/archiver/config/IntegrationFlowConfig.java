package fi.fmi.avi.archiver.config;

import static fi.fmi.avi.archiver.config.SpringLoggingContextHelper.getLoggingContext;
import static fi.fmi.avi.archiver.config.SpringLoggingContextHelper.withLoggingContext;
import static fi.fmi.avi.archiver.config.SpringLoggingContextHelper.withPayloadAndLoggingContext;
import static java.util.Objects.requireNonNull;
import static org.springframework.integration.file.FileHeaders.FILENAME;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

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
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.file.FileNameGenerator;
import org.springframework.integration.file.FileReadingMessageSource;
import org.springframework.integration.file.FileWritingMessageHandler;
import org.springframework.integration.file.filters.ChainFileListFilter;
import org.springframework.integration.file.filters.RegexPatternFileListFilter;
import org.springframework.integration.file.support.FileExistsMode;
import org.springframework.integration.file.transformer.FileToStringTransformer;
import org.springframework.integration.handler.advice.ExpressionEvaluatingRequestHandlerAdvice;
import org.springframework.integration.handler.advice.RequestHandlerRetryAdvice;
import org.springframework.integration.transformer.GenericTransformer;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableList;

import fi.fmi.avi.archiver.ProcessingState;
import fi.fmi.avi.archiver.config.model.AviationProduct;
import fi.fmi.avi.archiver.config.model.FileConfig;
import fi.fmi.avi.archiver.database.DatabaseService;
import fi.fmi.avi.archiver.file.FileMetadata;
import fi.fmi.avi.archiver.file.FileProcessingIdentifier;
import fi.fmi.avi.archiver.file.FileReference;
import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.logging.log4j.Log4JLoggables;
import fi.fmi.avi.archiver.logging.model.BulletinLogReference;
import fi.fmi.avi.archiver.logging.model.FileProcessingStatistics;
import fi.fmi.avi.archiver.logging.model.FileProcessingStatisticsImpl;
import fi.fmi.avi.archiver.logging.model.LoggingContext;
import fi.fmi.avi.archiver.logging.model.LoggingContextImpl;
import fi.fmi.avi.archiver.logging.model.ProcessingPhase;
import fi.fmi.avi.archiver.spring.context.CompoundLifecycle;
import fi.fmi.avi.archiver.spring.integration.dsl.ServiceActivators;
import fi.fmi.avi.archiver.spring.integration.file.filters.AcceptUnchangedFileListFilter;
import fi.fmi.avi.archiver.spring.integration.file.filters.ProcessingFileListFilter;
import fi.fmi.avi.archiver.spring.retry.RetryAdviceFactory;

@Configuration
public class IntegrationFlowConfig {

    public static final String FILE_METADATA = FileMetadata.class.getSimpleName();
    public static final String PROCESSING_ERRORS = "processingErrors";
    public static final String PROCESSING_IDENTIFIER = FileProcessingIdentifier.class.getSimpleName();

    private static final Logger LOGGER = LoggerFactory.getLogger(IntegrationFlowConfig.class);

    public static boolean hasProcessingErrors(final MessageHeaders headers) {
        return Optional.ofNullable(headers.get(PROCESSING_ERRORS, Boolean.class)).orElse(false);
    }

    @Bean
    IntegrationFlow archivalFlow(final FileToStringTransformer fileToStringTransformer, final RequestHandlerRetryAdvice fileReadingRetryAdvice,
            final ParserConfig.FileParserIntegrationService fileParserIntegrationService,
            final MessagePopulatorConfig.MessagePopulationIntegrationService messagePopulationIntegrationService, final DatabaseService databaseService,
            final MessageChannel processingChannel, final MessageChannel parserChannel, final MessageChannel populatorChannel,
            final MessageChannel databaseChannel, final MessageChannel archiveChannel, final MessageChannel successChannel, final MessageChannel failChannel) {
        return IntegrationFlows.from(processingChannel)
                .handle(ServiceActivators.execute(() -> Log4JLoggables.putMDC(ProcessingPhase.READ)))
                .transform(fileToStringTransformer, spec -> spec.advice(fileReadingRetryAdvice))
                .handle(ServiceActivators.execute(() -> Log4JLoggables.removeMDC(ProcessingPhase.READ)))
                .channel(parserChannel)
                .handle(ServiceActivators.execute(() -> Log4JLoggables.putMDC(ProcessingPhase.PARSE)))
                .<String> filter(content -> content != null && !content.isEmpty(), discards -> discards.discardChannel(failChannel))
                .handle(fileParserIntegrationService::parse)
                .handle(withLoggingContext(this::loggingActionsAfterParse))
                .<List<InputAviationMessage>> filter(messages -> !messages.isEmpty(), discards -> discards.discardChannel(failChannel))
                .handle(ServiceActivators.execute(() -> Log4JLoggables.removeMDC(ProcessingPhase.PARSE)))
                .channel(populatorChannel)
                .handle(ServiceActivators.execute(() -> Log4JLoggables.putMDC(ProcessingPhase.POPULATE)))
                .handle(messagePopulationIntegrationService::populateMessages)
                .handle(ServiceActivators.execute(() -> Log4JLoggables.removeMDC(ProcessingPhase.POPULATE)))
                .channel(databaseChannel)
                .handle(ServiceActivators.execute(() -> Log4JLoggables.putMDC(ProcessingPhase.STORE)))
                .handle(withPayloadAndLoggingContext(databaseService::insertMessages))
                .handle(ServiceActivators.execute(() -> Log4JLoggables.removeMDC(ProcessingPhase.STORE)))
                .channel(archiveChannel)
                .route(Message.class, message -> hasProcessingErrors(message.getHeaders()), spec -> spec//
                        .channelMapping(false, successChannel)//
                        .channelMapping(true, failChannel))
                .get();
    }

    private void loggingActionsAfterParse(final LoggingContext loggingContext) {
        loggingContext.initStatistics();
        logFileContentOverview(loggingContext);
    }

    private void logFileContentOverview(final LoggingContext loggingContext) {
        if (LOGGER.isInfoEnabled()) {
            final List<BulletinLogReference> bulletins = loggingContext.getAllBulletins();
            if (bulletins.size() == 1 && !bulletins.get(0).getHeading().isPresent()) {
                loggingContext.enterBulletin(0);
                LOGGER.info("Messages in <{}>: {}", loggingContext, loggingContext.getBulletinMessages());
                loggingContext.leaveBulletin();
            } else {
                LOGGER.info("Bulletins in <{}>: {}", loggingContext, bulletins);
            }
        }
    }

    @Bean
    IntegrationFlow finishFlow(final ProcessingState processingState, final MessageChannel finishChannel) {
        return IntegrationFlows.from(finishChannel)//
                .handle(ServiceActivators.execute(() -> Log4JLoggables.putMDC(ProcessingPhase.FINISH)))//
                .handle(ServiceActivators.peekHeader(FileMetadata.class, FILE_METADATA, processingState::finish))//
                .handle(this::logFinish)//
                .handle(ServiceActivators.execute(() -> Log4JLoggables.removeMDC(ProcessingPhase.FINISH)))//
                .nullChannel();
    }

    private Object logFinish(final Object payload, final MessageHeaders headers) {
        final LoggingContext loggingContext = getLoggingContext(headers);
        loggingContext.leaveBulletin();
        LOGGER.info("Finish processing <{}> {}. Statistics: {}", loggingContext, hasProcessingErrors(headers) ? "with errors" : "successfully",
                loggingContext.getStatistics());
        return payload;
    }

    @Bean
    IntegrationFlow errorMessageFlow(final MessageChannel errorMessageChannel, final MessageChannel failChannel,
            @SuppressWarnings("rawtypes") final GenericTransformer<Message, File> headerToFileTransformer,
            @SuppressWarnings("rawtypes") final GenericTransformer<Message, Message> errorMessageToOriginalTransformer) {
        return IntegrationFlows.from(errorMessageChannel)//
                .transform(Message.class, errorMessageToOriginalTransformer)//
                .transform(Message.class, headerToFileTransformer)//
                .enrichHeaders(spec -> spec.header(PROCESSING_ERRORS, true, true))//
                .channel(failChannel)//
                .get();
    }

    @Bean
    IntegrationFlow errorLoggingFlow(final MessageChannel errorLoggingChannel, final MessageChannel finishChannel,
            @SuppressWarnings("rawtypes") final GenericTransformer<Message, Message> errorMessageToOriginalTransformer) {
        return IntegrationFlows.from(errorLoggingChannel)//
                .transform(Message.class, errorMessageToOriginalTransformer)//
                .enrichHeaders(spec -> spec.header(PROCESSING_ERRORS, true, true))//
                .channel(finishChannel)//
                .get();
    }

    @Bean
    FileToStringTransformer fileToStringTransformer(@Value("${file-handler.charset}") final String charset) {
        final FileToStringTransformer transformer = new FileToStringTransformer();
        transformer.setCharset(charset);
        return transformer;
    }

    @Bean
    FileNameGenerator fileProcessingIdAppender() {
        return message -> message.getHeaders().get(FILENAME) + "." + message.getHeaders().get(PROCESSING_IDENTIFIER);
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

    @Bean
    @SuppressWarnings("rawtypes")
    GenericTransformer<Message, Message> errorMessageToOriginalTransformer() {
        return message -> {
            if (!(message instanceof ErrorMessage)) {
                return message;
            }
            final ErrorMessage errorMessage = (ErrorMessage) message;
            final Throwable throwable = errorMessage.getPayload();
            final Message<?> failedMessage;
            if (throwable instanceof MessagingException) {
                failedMessage = ((MessagingException) throwable).getFailedMessage();
            }
            // Attempt to use original message if the exception is not a MessagingException
            else if (errorMessage.getOriginalMessage() != null) {
                failedMessage = errorMessage.getOriginalMessage();
            } else {
                // Unable to get the original message, log the exception
                LOGGER.error("Unable to extract original Spring Integration message from error message of processing <{}>.", getLoggingContext(message),
                        throwable);
                failedMessage = null;
            }
            final LoggingContext loggingContext = getLoggingContext(failedMessage);
            final Throwable errorToLog = throwable instanceof MessagingException ? throwable.getCause() : throwable;
            LOGGER.error("Error while processing <{}>: {}", loggingContext, errorToLog == null ? "" : errorToLog.getMessage(), errorToLog);
            loggingContext.recordProcessingResult(FileProcessingStatistics.ProcessingResult.FAILED);
            return failedMessage;
        };
    }

    @Component
    static class ProductFlowsInitializer {
        private static final String PRODUCT_KEY = AviationProduct.class.getSimpleName();

        private final Set<IntegrationFlowContext.IntegrationFlowRegistration> registrations = new HashSet<>();

        private final IntegrationFlowContext context;
        private final Map<String, AviationProduct> aviationProducts;
        private final CompoundLifecycle inputReadersLifecycle;
        private final ProcessingState processingState;
        private final List<Advice> archiveAdviceChain;
        private final List<Advice> failAdviceChain;
        private final FileNameGenerator fileProcessingIdAppender;
        @SuppressWarnings("rawtypes")
        private final GenericTransformer<Message, File> headerToFileTransformer;
        private final Duration pollingDelay;
        private final MessageChannel processingChannel;
        private final MessageChannel errorMessageChannel;
        private final MessageChannel successChannel;
        private final MessageChannel failChannel;
        private final MessageChannel finishChannel;

        ProductFlowsInitializer(final IntegrationFlowContext context, final Map<String, AviationProduct> aviationProducts,
                final CompoundLifecycle inputReadersLifecycle, final ProcessingState processingState, final List<Advice> archiveAdviceChain,
                final List<Advice> failAdviceChain, final FileNameGenerator fileProcessingIdAppender,
                @SuppressWarnings("rawtypes") final GenericTransformer<Message, File> headerToFileTransformer,
                @Value("${polling.delay}") final Duration pollingDelay, final MessageChannel processingChannel, final MessageChannel errorMessageChannel,
                final MessageChannel successChannel, final MessageChannel failChannel, final MessageChannel finishChannel) {
            this.context = requireNonNull(context, "context");
            this.aviationProducts = requireNonNull(aviationProducts, "aviationProducts");
            this.inputReadersLifecycle = requireNonNull(inputReadersLifecycle, "inputReadersLifecycle");
            this.processingState = requireNonNull(processingState, "processingState");
            this.archiveAdviceChain = requireNonNull(archiveAdviceChain, "archiveAdviceChain");
            this.failAdviceChain = requireNonNull(failAdviceChain, "failAdviceChain");
            this.fileProcessingIdAppender = requireNonNull(fileProcessingIdAppender, "fileProcessingIdAppender");
            this.headerToFileTransformer = requireNonNull(headerToFileTransformer, "headerToFileTransformer");
            this.pollingDelay = requireNonNull(pollingDelay, "pollingDelay");
            this.processingChannel = requireNonNull(processingChannel, "processingChannel");
            this.errorMessageChannel = requireNonNull(errorMessageChannel, "errorMessageChannel");
            this.successChannel = requireNonNull(successChannel, "successChannel");
            this.failChannel = requireNonNull(failChannel, "failChannel");
            this.finishChannel = requireNonNull(finishChannel, "finishChannel");
        }

        private static <T> T getNonNullHeader(final MessageHeaders headers, final Object key, final Class<T> type) {
            final T header = headers.get(key, type);
            assert header != null : key;
            return header;
        }

        private static FileMetadata createFileMetadata(final Message<?> message, final FileConfig fileConfig, final String productIdentifier) {
            final String filename = getNonNullHeader(message.getHeaders(), FILENAME, String.class);
            return FileMetadata.builder()
                    .setFileReference(FileReference.create(productIdentifier, filename))
                    .setFileConfig(fileConfig)
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
                registerIntegrationFlow(
                        IntegrationFlows.from(sourceReader, adapterSpec -> adapterSpec.poller(Pollers.fixedDelay(pollingDelay).maxMessagesPerPoll(-1)))
                                .channel(inputChannel)//
                                .get());

                // Integration flow for file name filtering
                registerAllIntegrationFlows(product.getFileConfigs().stream()//
                        .map(fileConfig -> IntegrationFlows.from(inputChannel)//
                                .handle(ServiceActivators.execute(() -> Log4JLoggables.putMDC(ProcessingPhase.START)))//
                                .filter(new RegexPatternFileListFilter(fileConfig.getPattern())::accept)//
                                .enrichHeaders(spec -> spec//
                                        .header(PRODUCT_KEY, product)//
                                        .errorChannel(errorMessageChannel)
                                        .headerFunction(FILE_METADATA, message -> createFileMetadata(message, fileConfig, product.getId()))//
                                        .headerFunction(PROCESSING_IDENTIFIER, message -> FileProcessingIdentifier.newInstance()))//
                                .enrichHeaders(spec -> spec//
                                        // New header enrichment is required to refer header values set earlier
                                        .headerFunction(SpringLoggingContextHelper.HEADER_KEY, message -> createLoggingContext(
                                                getNonNullHeader(message.getHeaders(), PROCESSING_IDENTIFIER, FileProcessingIdentifier.class))))//
                                .handle((payload, headers) -> {
                                    final FileMetadata fileMetadata = getNonNullHeader(headers, FILE_METADATA, FileMetadata.class);
                                    processingState.start(fileMetadata);
                                    final LoggingContext loggingContext = getLoggingContext(headers);
                                    loggingContext.enterFile(fileMetadata.getFileReference());
                                    LOGGER.info("Start processing <{}>", loggingContext);
                                    return payload;
                                })//
                                .handle(ServiceActivators.execute(() -> Log4JLoggables.removeMDC(ProcessingPhase.START)))//
                                .channel(processingChannel)//
                                .get()//
                        ));

                @SuppressWarnings("rawtypes")
                final GenericSelector<Message> productFilter = m -> Objects.equals(m.getHeaders().get(PRODUCT_KEY), product);

                registerIntegrationFlow(IntegrationFlows.from(successChannel)//
                        .handle(ServiceActivators.execute(() -> Log4JLoggables.putMDC(ProcessingPhase.SUCCESS)))//
                        .filter(Message.class, productFilter)//
                        .transform(Message.class, headerToFileTransformer)//
                        .handle(createArchiveHandler(product.getArchiveDir()))//
                        .handle(withLoggingContext(loggingContext -> LOGGER.debug("Moved <{}> to '{}'.", loggingContext, product.getArchiveDir())))//
                        .handle(ServiceActivators.execute(() -> Log4JLoggables.removeMDC(ProcessingPhase.SUCCESS)))//
                        .channel(finishChannel)//
                        .get());

                registerIntegrationFlow(IntegrationFlows.from(failChannel)//
                        .handle(ServiceActivators.execute(() -> Log4JLoggables.putMDC(ProcessingPhase.FAIL)))//
                        .filter(Message.class, productFilter)//
                        .transform(Message.class, headerToFileTransformer)//
                        .handle(createFailHandler(product.getFailDir()))//
                        .handle(withLoggingContext(loggingContext -> LOGGER.debug("Moved <{}> to '{}'.", loggingContext, product.getFailDir())))//
                        .handle(ServiceActivators.execute(() -> Log4JLoggables.removeMDC(ProcessingPhase.FAIL)))//
                        .channel(finishChannel)//
                        .get());
            });
        }

        private LoggingContext createLoggingContext(final FileProcessingIdentifier fileProcessingIdentifier) {
            return LoggingContext.asSynchronized(
                    new LoggingContextImpl(fileProcessingIdentifier, FileProcessingStatistics.asSynchronized(new FileProcessingStatisticsImpl())));
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

        private FileReadingMessageSource createMessageSource(final Path inputDir, final String productId) {
            final FileReadingMessageSource sourceDirectory = new FileReadingMessageSource();
            sourceDirectory.setDirectory(inputDir.toFile());
            sourceDirectory.setFilter(createSourceFileListFilter(productId));
            return sourceDirectory;
        }

        private ChainFileListFilter<File> createSourceFileListFilter(final String productId) {
            return new ChainFileListFilter<>(ImmutableList.of(//
                    new ProcessingFileListFilter(processingState, productId), //
                    new AcceptUnchangedFileListFilter()));
        }

        private FileWritingMessageHandler createArchiveHandler(final Path destinationDir) {
            final FileWritingMessageHandler archiveHandler = new FileWritingMessageHandler(destinationDir.toFile());
            archiveHandler.setFileExistsMode(FileExistsMode.REPLACE_IF_MODIFIED);
            archiveHandler.setDeleteSourceFiles(true);
            archiveHandler.setAdviceChain(archiveAdviceChain);
            archiveHandler.setFileNameGenerator(fileProcessingIdAppender);
            return archiveHandler;
        }

        private FileWritingMessageHandler createFailHandler(final Path destinationDir) {
            final FileWritingMessageHandler failHandler = new FileWritingMessageHandler(destinationDir.toFile());
            failHandler.setFileExistsMode(FileExistsMode.REPLACE_IF_MODIFIED);
            failHandler.setDeleteSourceFiles(true);
            failHandler.setAdviceChain(failAdviceChain);
            failHandler.setFileNameGenerator(fileProcessingIdAppender);
            return failHandler;
        }
    }

}
