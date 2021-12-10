package fi.fmi.avi.archiver.config;

import com.google.common.collect.ImmutableList;
import fi.fmi.avi.archiver.ProcessingState;
import fi.fmi.avi.archiver.database.DatabaseService;
import fi.fmi.avi.archiver.file.FileConfig;
import fi.fmi.avi.archiver.file.FileMetadata;
import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.initializing.AviationProductsHolder;
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
import org.springframework.beans.factory.annotation.Autowired;
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

@Configuration
public class IntegrationFlowConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(IntegrationFlowConfig.class);

    public static final String FILE_METADATA = "file_metadata";
    public static final String FAILED_MESSAGES = "processing_failures";
    public static final String DISCARDED_MESSAGES = "processing_discards";
    public static final String FILE_PARSE_ERRORS = "file_parsed_partially";

    private static final String PRODUCT_KEY = "product";
    private static final String INPUT_CATEGORY = "input";

    @Value("${file-handler.retry.initial-interval}")
    private Duration initialInterval;

    @Value("${file-handler.retry.max-interval}")
    private Duration maxInterval;

    @Value("${file-handler.retry.multiplier}")
    private int retryMultiplier;

    @Value("${file-handler.retry.timeout}")
    private Duration timeout;

    @Value("${polling.filter-queue-size}")
    private int filterQueueSize;

    @Value("${file-handler.charset}")
    private String charset;

    @Autowired
    private IntegrationFlowContext context;

    @Autowired
    private Clock clock;

    @Autowired
    private MessageChannel processingChannel;

    @Autowired
    private MessageChannel parserChannel;

    @Autowired
    private MessageChannel populatorChannel;

    @Autowired
    private MessageChannel databaseChannel;

    @Autowired
    private MessageChannel archiveChannel;

    @Autowired
    private MessageChannel successChannel;

    @Autowired
    private MessageChannel failChannel;

    @Autowired
    private MessageChannel finishChannel;

    @Autowired
    private MessageChannel errorMessageChannel;

    @Autowired
    private MessageChannel errorLoggingChannel;

    @Autowired
    private ParserConfig.FileParserService fileParserService;

    @Autowired
    public MessagePopulatorService messagePopulatorService;

    @Autowired
    private DatabaseService databaseService;

    @Autowired
    private AviationProductsHolder aviationProductsHolder;

    @Autowired
    private ProcessingState processingState;

    @Autowired
    private CompoundLifecycle inputReadersLifecycle;

    private final Set<IntegrationFlowContext.IntegrationFlowRegistration> registrations = new HashSet<>();

    @Bean
    public IntegrationFlow archivalFlow() {
        return IntegrationFlows.from(processingChannel)
                .transform(Message.class, msg -> fileToStringTransformer().transform(msg), spec -> spec.advice(fileReadingRetryAdvice()))
                .channel(parserChannel)
                .<String>filter(content -> content != null && !content.isEmpty(), discards -> discards.discardChannel(failChannel))
                .<String>handle((payload, headers) -> fileParserService.parse(payload, headers))
                .<List<InputAviationMessage>>filter(messages -> !messages.isEmpty(), discards -> discards.discardChannel(failChannel))
                .channel(populatorChannel)
                .<List<InputAviationMessage>>handle((payload, headers) -> messagePopulatorService.populateMessages(payload, headers))
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
    public IntegrationFlow logSuccessFlow() {
        return IntegrationFlows.from(successChannel).log("Archive").get();
    }

    @Bean
    public IntegrationFlow finishFlow() {
        return IntegrationFlows.from(finishChannel)//
                .handle(ServiceActivators.peekHeader(FileMetadata.class, FILE_METADATA, processingState::finish))//
                .nullChannel();
    }

    @Bean
    public IntegrationFlow errorMessageFlow() {
        return IntegrationFlows.from(errorMessageChannel)//
                .handle(ErrorMessage.class, (payload, headers) -> errorMessageToOriginalMessage(payload))
                .transform(Message.class, m -> m.getHeaders().get(FileHeaders.ORIGINAL_FILE, File.class))//
                .channel(failChannel)//
                .get();
    }

    @Bean
    public IntegrationFlow errorLoggingFlow() {
        return IntegrationFlows.from(errorLoggingChannel)//
                .handle(ErrorMessage.class, (payload, headers) -> errorMessageToOriginalMessage(payload))
                .channel(finishChannel)//
                .get();
    }

    @Nullable
    public Message<?> errorMessageToOriginalMessage(final ErrorMessage errorMessage) {
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
    public FileToStringTransformer fileToStringTransformer() {
        final FileToStringTransformer transformer = new FileToStringTransformer();
        transformer.setCharset(charset);
        return transformer;
    }

    @Bean
    public FileNameGenerator timestampAppender() {
        return msg -> msg.getHeaders().get(FileHeaders.FILENAME) + "." + clock.millis();
    }

    // Trap exceptions to avoid infinite looping when the error message flow itself results in exceptions
    @Bean
    public Advice exceptionTrapAdvice() {
        final ExpressionEvaluatingRequestHandlerAdvice advice = new ExpressionEvaluatingRequestHandlerAdvice();
        advice.setFailureChannel(errorLoggingChannel);
        advice.setTrapException(true);
        return advice;
    }

    @Bean
    public RequestHandlerRetryAdvice fileReadingRetryAdvice() {
        return RetryAdviceFactory.create("File reading", initialInterval, maxInterval, retryMultiplier, timeout);
    }

    @Bean
    public RequestHandlerRetryAdvice archiveRetryAdvice() {
        return RetryAdviceFactory.create("Writing to archive dir", initialInterval, maxInterval, retryMultiplier, timeout);
    }

    @Bean
    public RequestHandlerRetryAdvice failRetryAdvice() {
        return RetryAdviceFactory.create("Writing to fail dir", initialInterval, maxInterval, retryMultiplier, timeout);
    }

    @Bean
    public List<Advice> archiveAdviceChain() {
        return ImmutableList.of(exceptionTrapAdvice(), archiveRetryAdvice());
    }

    @Bean
    public List<Advice> failAdviceChain() {
        return ImmutableList.of(exceptionTrapAdvice(), failRetryAdvice());
    }

    @Bean
    @SuppressWarnings("rawtypes")
    public GenericTransformer<Message, File> headerToFileTransformer() {
        return message -> message.getHeaders().get(FileHeaders.ORIGINAL_FILE, File.class);
    }

    @PostConstruct
    private void initializeProductFlows() {
        aviationProductsHolder.getProducts().values().forEach(product -> {
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

            final GenericSelector<Message> productFilter = m -> Objects.equals(m.getHeaders().get(PRODUCT_KEY), product);

            registerIntegrationFlow(IntegrationFlows.from(successChannel)//
                    .filter(Message.class, productFilter)//
                    .transform(Message.class, headerToFileTransformer())//
                    .handle(createArchiveHandler(product.getArchiveDir()))//
                    .channel(finishChannel)//
                    .get());

            registerIntegrationFlow(IntegrationFlows.from(failChannel)//
                    .filter(Message.class, productFilter)//
                    .transform(Message.class, headerToFileTransformer())//
                    .handle(createFailHandler(product.getFailDir()))//
                    .channel(finishChannel)//
                    .get());
        });
    }

    @PreDestroy
    public void dispose() {
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
        sourceDirectory.setFilter(new ChainFileListFilter<>(
                ImmutableList.of(new ProcessingFileListFilter(processingState, productId),
                        new AcceptUnchangedFileListFilter(), new AcceptOnceFileListFilter<>(filterQueueSize))));
        return sourceDirectory;
    }

    private FileWritingMessageHandler createArchiveHandler(final File destinationDir) {
        final FileWritingMessageHandler archiveHandler = new FileWritingMessageHandler(destinationDir);
        archiveHandler.setFileExistsMode(FileExistsMode.REPLACE_IF_MODIFIED);
        archiveHandler.setDeleteSourceFiles(true);
        archiveHandler.setAdviceChain(archiveAdviceChain());
        archiveHandler.setFileNameGenerator(timestampAppender());
        return archiveHandler;
    }

    private FileWritingMessageHandler createFailHandler(final File destinationDir) {
        final FileWritingMessageHandler failHandler = new FileWritingMessageHandler(destinationDir);
        failHandler.setFileExistsMode(FileExistsMode.REPLACE_IF_MODIFIED);
        failHandler.setDeleteSourceFiles(true);
        failHandler.setAdviceChain(failAdviceChain());
        failHandler.setFileNameGenerator(timestampAppender());
        return failHandler;
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

}
