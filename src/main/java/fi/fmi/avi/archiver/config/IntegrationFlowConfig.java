package fi.fmi.avi.archiver.config;

import static fi.fmi.avi.archiver.config.SpringLoggingContextHelper.getLoggingContext;
import static fi.fmi.avi.archiver.config.SpringLoggingContextHelper.withLoggingContext;
import static fi.fmi.avi.archiver.config.SpringLoggingContextHelper.withPayloadAndLoggingContext;
import static fi.fmi.avi.archiver.logging.GenericStructuredLoggable.loggable;
import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.aopalliance.aop.Advice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
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
import org.springframework.integration.handler.GenericHandler;
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
import fi.fmi.avi.archiver.logging.GenericStructuredLoggable;
import fi.fmi.avi.archiver.logging.model.BulletinLogReference;
import fi.fmi.avi.archiver.logging.model.FileProcessingStatistics;
import fi.fmi.avi.archiver.logging.model.FileProcessingStatisticsImpl;
import fi.fmi.avi.archiver.logging.model.LoggingContext;
import fi.fmi.avi.archiver.logging.model.LoggingContextImpl;
import fi.fmi.avi.archiver.logging.model.ProcessingPhase;
import fi.fmi.avi.archiver.logging.slf4j.SLF4JLoggables;
import fi.fmi.avi.archiver.spring.context.CompoundLifecycle;
import fi.fmi.avi.archiver.spring.integration.dsl.ServiceActivators;
import fi.fmi.avi.archiver.spring.integration.file.filters.AcceptUnchangedFileListFilter;
import fi.fmi.avi.archiver.spring.integration.file.filters.AnyAcceptFileListFilter;
import fi.fmi.avi.archiver.spring.integration.file.filters.ProcessingFileListFilter;
import fi.fmi.avi.archiver.spring.messaging.MessageHeaderReference;
import fi.fmi.avi.archiver.spring.retry.RetryAdviceFactory;

@Configuration
public class IntegrationFlowConfig {
    public static final MessageHeaderReference<FileReference> FILE_REFERENCE = MessageHeaderReference.simpleNameOf(FileReference.class);
    public static final MessageHeaderReference<FileMetadata> FILE_METADATA = MessageHeaderReference.simpleNameOf(FileMetadata.class);
    public static final MessageHeaderReference<Boolean> PROCESSING_ERRORS = MessageHeaderReference.of("ProcessingErrors", Boolean.class);
    public static final MessageHeaderReference<FileProcessingIdentifier> PROCESSING_IDENTIFIER = MessageHeaderReference.simpleNameOf(
            FileProcessingIdentifier.class);
    public static final MessageHeaderReference<String> FILENAME = MessageHeaderReference.of(FileHeaders.FILENAME, String.class);
    public static final MessageHeaderReference<File> ORIGINAL_FILE = MessageHeaderReference.of(FileHeaders.ORIGINAL_FILE, File.class);

    private static final Logger LOGGER = LoggerFactory.getLogger(IntegrationFlowConfig.class);
    private static final String PROCESSING_ERRORS_LOGGABLE = "processingErrors";
    private static final GenericStructuredLoggable<Boolean> PROCESSING_ERRORS_TRUE = loggable(PROCESSING_ERRORS_LOGGABLE, true, "with errors");
    private static final GenericStructuredLoggable<Boolean> PROCESSING_ERRORS_FALSE = loggable(PROCESSING_ERRORS_LOGGABLE, false, "successfully");

    private static final List<String> LOGGING_ENV_MDC_KEYS = ImmutableList.of(//
            FileProcessingIdentifier.newInstance().getStructureName(), //
            ProcessingPhase.START.getStructureName());

    public static boolean hasProcessingErrors(final MessageHeaders headers) {
        return PROCESSING_ERRORS.getOptional(headers).orElse(false);
    }

    private static GenericHandler<?> loggingEnvSetter(final ProcessingPhase processingPhase) {
        return (payload, headers) -> {
            setLoggingEnv(headers, processingPhase);
            return payload;
        };
    }

    private static GenericHandler<?> loggingEnvCleaner() {
        return ServiceActivators.execute(IntegrationFlowConfig::unsetLoggingEnv);
    }

    private static void setLoggingEnv(final MessageHeaders headers, final ProcessingPhase processingPhase) {
        PROCESSING_IDENTIFIER.getOptional(headers)//
                .ifPresent(SLF4JLoggables::putMDC);
        SLF4JLoggables.putMDC(processingPhase);
    }

    private static void unsetLoggingEnv() {
        LOGGING_ENV_MDC_KEYS.forEach(MDC::remove);
    }

    @Bean
    IntegrationFlow archivalFlow(final FileToStringTransformer fileToStringTransformer, final RequestHandlerRetryAdvice fileReadingRetryAdvice,
            final ParserConfig.FileParserIntegrationService fileParserIntegrationService,
            final MessagePopulatorConfig.MessagePopulationIntegrationService messagePopulationIntegrationService, final DatabaseService databaseService,
            final MessageChannel processingChannel, final MessageChannel parserChannel, final MessageChannel populatorChannel,
            final MessageChannel databaseChannel, final MessageChannel archiveChannel, final MessageChannel successChannel, final MessageChannel failChannel) {
        return IntegrationFlows.from(processingChannel)
                .handle(loggingEnvSetter(ProcessingPhase.READ))
                .transform(fileToStringTransformer, spec -> spec.advice(fileReadingRetryAdvice))
                .handle(loggingEnvCleaner())
                .channel(parserChannel)
                .handle(loggingEnvSetter(ProcessingPhase.PARSE))
                .<String> filter(content -> content != null && !content.isEmpty(), discards -> discards.discardChannel(failChannel))
                .handle(fileParserIntegrationService::parse)
                .handle(withLoggingContext(this::loggingActionsAfterParse))
                .<List<InputAviationMessage>> filter(messages -> !messages.isEmpty(), discards -> discards.discardChannel(failChannel))
                .handle(loggingEnvCleaner())
                .channel(populatorChannel)
                .handle(loggingEnvSetter(ProcessingPhase.POPULATE))
                .handle(messagePopulationIntegrationService::populateMessages)
                .handle(loggingEnvCleaner())
                .channel(databaseChannel)
                .handle(loggingEnvSetter(ProcessingPhase.STORE))
                .handle(withPayloadAndLoggingContext(databaseService::insertMessages))
                .handle(loggingEnvCleaner())
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
                .handle(loggingEnvSetter(ProcessingPhase.FINISH))//
                .handle(ServiceActivators.peekHeader(FILE_REFERENCE, processingState::finish))//
                .handle(this::logFinish)//
                .handle(loggingEnvCleaner())//
                .nullChannel();
    }

    private Object logFinish(final Object payload, final MessageHeaders headers) {
        final LoggingContext loggingContext = getLoggingContext(headers);
        loggingContext.leaveBulletin();
        LOGGER.info("Finish processing <{}> {}. Statistics: {}", loggingContext,
                hasProcessingErrors(headers) ? PROCESSING_ERRORS_TRUE : PROCESSING_ERRORS_FALSE, loggingContext.getStatistics());
        return payload;
    }

    @Bean
    IntegrationFlow errorMessageFlow(final MessageChannel errorMessageChannel, final MessageChannel failChannel,
            @SuppressWarnings("rawtypes") final GenericTransformer<Message, File> headerToFileTransformer,
            @SuppressWarnings("rawtypes") final GenericTransformer<Message, Message> errorMessageToOriginalTransformer) {
        return IntegrationFlows.from(errorMessageChannel)//
                .handle(loggingEnvSetter(ProcessingPhase.FAIL))//
                .transform(Message.class, errorMessageToOriginalTransformer)//
                .transform(Message.class, headerToFileTransformer)//
                .enrichHeaders(spec -> spec.header(PROCESSING_ERRORS.getName(), true, true))//
                .handle(loggingEnvCleaner())//
                .channel(failChannel)//
                .get();
    }

    @Bean
    IntegrationFlow errorLoggingFlow(final MessageChannel errorLoggingChannel, final MessageChannel finishChannel,
            @SuppressWarnings("rawtypes") final GenericTransformer<Message, Message> errorMessageToOriginalTransformer) {
        return IntegrationFlows.from(errorLoggingChannel)//
                .handle(loggingEnvSetter(ProcessingPhase.FAIL))//
                .transform(Message.class, errorMessageToOriginalTransformer)//
                .enrichHeaders(spec -> spec.header(PROCESSING_ERRORS.getName(), true, true))//
                .handle(loggingEnvCleaner())//
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
        return message -> FILENAME.getNullable(message.getHeaders()) + "." + PROCESSING_IDENTIFIER.getNullable(message.getHeaders());
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
        return message -> ORIGINAL_FILE.getNullable(message.getHeaders());
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
            if (failedMessage != null) {
                setLoggingEnv(failedMessage.getHeaders(), ProcessingPhase.FAIL);
            }
            final LoggingContext loggingContext = getLoggingContext(failedMessage);
            @Nullable
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

        private static FileMetadata createFileMetadata(final Message<?> message, final FileConfig fileConfig) {
            return FileMetadata.builder()//
                    .setFileReference(FILE_REFERENCE.getNonNull(message.getHeaders()))//
                    .setFileConfig(fileConfig)//
                    .setFileModified(getFileModified(message))//
                    .build();
        }

        private static Optional<Instant> getFileModified(final Message<?> message) {
            return ORIGINAL_FILE.getOptional(message.getHeaders())//
                    .map(file -> {
                        try {
                            return Files.getLastModifiedTime(file.toPath()).toInstant();
                        } catch (final IOException e) {
                            LOGGER.error("Unable to get file last modified time: {}", file.getName(), e);
                        }
                        return null;
                    });
        }

        @PostConstruct
        void initializeProductFlows() {
            aviationProducts.values().forEach(product -> {
                final FileReadingMessageSource sourceReader = createMessageSource(product);
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
                                .filter(new RegexPatternFileListFilter(fileConfig.getPattern())::accept)//
                                .enrichHeaders(spec -> spec//
                                        .header(PRODUCT_KEY, product)//
                                        .errorChannel(errorMessageChannel)
                                        .headerFunction(FILE_REFERENCE.getName(),
                                                message -> FileReference.create(product.getId(), FILENAME.getNonNull(message.getHeaders())))
                                        .headerFunction(PROCESSING_IDENTIFIER.getName(), message -> FileProcessingIdentifier.newInstance())//
                                        .headerFunction(SpringLoggingContextHelper.HEADER.getName(), message -> createLoggingContext()))//
                                .handle(loggingEnvSetter(ProcessingPhase.START))//
                                .handle((payload, headers) -> {
                                    final FileReference file = FILE_REFERENCE.getNonNull(headers);
                                    processingState.start(file);
                                    final LoggingContext loggingContext = getLoggingContext(headers);
                                    loggingContext.enterFile(file);
                                    LOGGER.info("Start processing <{}>", loggingContext);
                                    return payload;
                                })//
                                .enrichHeaders(spec -> spec//
                                        .headerFunction(FILE_METADATA.getName(), message -> createFileMetadata(message, fileConfig)))//
                                .handle(loggingEnvCleaner())//
                                .channel(processingChannel)//
                                .get()//
                        ));

                @SuppressWarnings("rawtypes")
                final GenericSelector<Message> productFilter = m -> Objects.equals(m.getHeaders().get(PRODUCT_KEY), product);

                registerIntegrationFlow(IntegrationFlows.from(successChannel)//
                        .handle(loggingEnvSetter(ProcessingPhase.SUCCESS))//
                        .filter(Message.class, productFilter)//
                        .transform(Message.class, headerToFileTransformer)//
                        .handle(createArchiveHandler(product.getArchiveDir()))//
                        .handle(withLoggingContext(loggingContext -> LOGGER.debug("Moved <{}> to '{}'.", loggingContext, product.getArchiveDir())))//
                        .handle(loggingEnvCleaner())//
                        .channel(finishChannel)//
                        .get());

                registerIntegrationFlow(IntegrationFlows.from(failChannel)//
                        .handle(loggingEnvSetter(ProcessingPhase.FAIL))//
                        .filter(Message.class, productFilter)//
                        .transform(Message.class, headerToFileTransformer)//
                        .handle(createFailHandler(product.getFailDir()))//
                        .handle(withLoggingContext(loggingContext -> LOGGER.debug("Moved <{}> to '{}'.", loggingContext, product.getFailDir())))//
                        .handle(loggingEnvCleaner())//
                        .channel(finishChannel)//
                        .get());
            });
        }

        private LoggingContext createLoggingContext() {
            return LoggingContext.asSynchronized(new LoggingContextImpl(FileProcessingStatistics.asSynchronized(new FileProcessingStatisticsImpl())));
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

        private FileReadingMessageSource createMessageSource(final AviationProduct product) {
            final FileReadingMessageSource source = new FileReadingMessageSource();
            source.setDirectory(product.getInputDir().toFile());
            source.setFilter(createSourceFileListFilter(product));
            return source;
        }

        private ChainFileListFilter<File> createSourceFileListFilter(final AviationProduct product) {
            return new ChainFileListFilter<>(Arrays.asList(//
                    new ProcessingFileListFilter(processingState, product.getId()), //
                    new AnyAcceptFileListFilter<>(product.getFileConfigs().stream()//
                            .map(fileConfig -> new RegexPatternFileListFilter(fileConfig.getPattern()))//
                            .collect(Collectors.toList())), //
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
