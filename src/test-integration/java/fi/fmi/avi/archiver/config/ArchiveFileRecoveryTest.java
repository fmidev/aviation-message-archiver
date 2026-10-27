package fi.fmi.avi.archiver.config;

import fi.fmi.avi.archiver.ProcessingState;
import fi.fmi.avi.archiver.config.IntegrationFlowConfig.ProductFlowsInitializer;
import fi.fmi.avi.archiver.file.FileProcessingIdentifier;
import fi.fmi.avi.archiver.spring.context.CompoundLifecycle;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.integration.channel.NullChannel;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.file.FileNameGenerator;
import org.springframework.integration.file.FileWritingMessageHandler;
import org.springframework.integration.handler.advice.RequestHandlerRetryAdvice;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.retry.support.RetryTemplate;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static fi.fmi.avi.archiver.config.IntegrationFlowConfig.FILENAME;
import static fi.fmi.avi.archiver.config.IntegrationFlowConfig.ORIGINAL_FILE;
import static fi.fmi.avi.archiver.config.IntegrationFlowConfig.PROCESSING_IDENTIFIER;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;

class ArchiveFileRecoveryTest {
    private static final String SOURCE_FILENAME = "weather.aviation.metar-ESNV-20261016T205000Z.xml";
    private static final String HANDLER_FACTORIES_SOURCE = "handlerFactories";
    private static final FileTime FILE_MODIFIED = FileTime.from(Instant.parse("2026-10-16T20:53:00Z"));

    private FileNameGenerator fileNameGenerator;
    private ProductFlowsInitializer initializer;
    private @Nullable FileWritingMessageHandler handler;

    private static Stream<Named<BiFunction<ProductFlowsInitializer, Path, FileWritingMessageHandler>>> handlerFactories() {
        return Stream.of(
                Named.of("archive", ProductFlowsInitializer::createArchiveHandler),
                Named.of("fail", ProductFlowsInitializer::createFailHandler));
    }

    private static Stream<Arguments> handlerFactoriesAndModificationTimes() {
        return handlerFactories().flatMap(factory -> Stream.of(
                        Named.of("same modification time", FILE_MODIFIED),
                        Named.of("earlier modification time", FileTime.from(FILE_MODIFIED.toInstant().minusSeconds(60))),
                        Named.of("later modification time", FileTime.from(FILE_MODIFIED.toInstant().plusSeconds(60))))
                .map(modified -> Arguments.of(factory, modified)));
    }

    private static Message<File> messageFor(final Path source, final FileProcessingIdentifier processingIdentifier) {
        return MessageBuilder.withPayload(source.toFile())
                .setHeader(FILENAME.getName(), source.getFileName().toString())
                .setHeader(ORIGINAL_FILE.getName(), source.toFile())
                .setHeader(PROCESSING_IDENTIFIER.getName(), processingIdentifier)
                .build();
    }

    @BeforeEach
    void setUp() {
        final IntegrationFlowConfig config = new IntegrationFlowConfig();
        fileNameGenerator = config.fileProcessingIdAppender();
        final RequestHandlerRetryAdvice retryAdvice = new RequestHandlerRetryAdvice();
        // Bound retries only in the test, so the regression fails instead of hanging indefinitely.
        retryAdvice.setRetryTemplate(RetryTemplate.builder().maxAttempts(3).noBackoff().build());
        final NullChannel unusedChannel = new NullChannel();
        initializer = new ProductFlowsInitializer(
                mock(IntegrationFlowContext.class), Map.of(), new CompoundLifecycle(), new ProcessingState(Clock.systemUTC()),
                List.of(retryAdvice), List.of(retryAdvice), fileNameGenerator, config.headerToFileTransformer(),
                Duration.ofSeconds(15), unusedChannel, unusedChannel, unusedChannel, unusedChannel, unusedChannel, unusedChannel);
    }

    private FileWritingMessageHandler createHandler(
            final BiFunction<ProductFlowsInitializer, Path, FileWritingMessageHandler> handlerFactory, final Path destinationDir) {
        final FileWritingMessageHandler handler = handlerFactory.apply(initializer, destinationDir);
        this.handler = handler;
        handler.setBeanFactory(new DefaultListableBeanFactory());
        handler.setOutputChannel(new NullChannel());
        handler.afterPropertiesSet();
        return handler;
    }

    @AfterEach
    void tearDown() {
        if (handler != null) {
            handler.destroy();
        }
    }

    @ParameterizedTest
    @MethodSource(HANDLER_FACTORIES_SOURCE)
    void retrying_a_completed_move_succeeds_when_the_source_is_missing(
            final BiFunction<ProductFlowsInitializer, Path, FileWritingMessageHandler> handlerFactory, @TempDir final Path workDir) throws Exception {
        final String content = "<METAR>test message</METAR>";
        final Path source = Files.writeString(workDir.resolve(SOURCE_FILENAME), content);
        Files.setLastModifiedTime(source, FILE_MODIFIED);
        final Path destinationDir = Files.createDirectory(workDir.resolve("destination"));
        final FileProcessingIdentifier processingIdentifier = FileProcessingIdentifier.newInstance();
        final Message<File> message = messageFor(source, processingIdentifier);
        final Path destination = destinationDir.resolve(fileNameGenerator.generateFileName(message));
        final FileWritingMessageHandler handler = createHandler(handlerFactory, destinationDir);
        handler.handleMessage(message);
        assertThat(source).doesNotExist();
        assertThat(destination).hasContent(content);

        // A filesystem operation may complete even though its caller observes a failure.
        // Re-deliver the unchanged message to reproduce the state seen by the next retry.
        assertThatCode(() -> handler.handleMessage(message))
                .as("Retrying a completed move should recognize the existing destination and finish")
                .doesNotThrowAnyException();

        assertThat(source).doesNotExist();
        assertThat(destination).hasContent(content);
    }

    @ParameterizedTest
    @MethodSource(HANDLER_FACTORIES_SOURCE)
    void missing_source_and_destination_are_not_reported_as_a_completed_move(
            final BiFunction<ProductFlowsInitializer, Path, FileWritingMessageHandler> handlerFactory, @TempDir final Path workDir) {
        final Path source = workDir.resolve(SOURCE_FILENAME);
        final Message<File> message = messageFor(source, FileProcessingIdentifier.newInstance());
        final FileWritingMessageHandler handler = createHandler(handlerFactory, workDir);
        assertThatThrownBy(() -> handler.handleMessage(message))
                .hasRootCauseInstanceOf(NoSuchFileException.class);
    }

    @ParameterizedTest
    @MethodSource(HANDLER_FACTORIES_SOURCE)
    void a_temporary_file_does_not_count_as_a_completed_move(
            final BiFunction<ProductFlowsInitializer, Path, FileWritingMessageHandler> handlerFactory, @TempDir final Path workDir) throws Exception {
        final String partialContent = "partial content";
        final Path source = workDir.resolve(SOURCE_FILENAME);
        final FileProcessingIdentifier processingIdentifier = FileProcessingIdentifier.newInstance();
        final Message<File> message = messageFor(source, processingIdentifier);
        final String destinationFilename = fileNameGenerator.generateFileName(message);
        final Path destination = workDir.resolve(destinationFilename);
        final Path temporaryFile = Files.writeString(workDir.resolve(destinationFilename + ".writing"), partialContent);
        final FileWritingMessageHandler handler = createHandler(handlerFactory, workDir);
        assertThatThrownBy(() -> handler.handleMessage(message))
                .hasRootCauseInstanceOf(NoSuchFileException.class);
        assertThat(destination).doesNotExist();
        assertThat(temporaryFile).hasContent(partialContent);
    }

    @ParameterizedTest
    @MethodSource(HANDLER_FACTORIES_SOURCE)
    void a_file_from_another_processing_attempt_does_not_count_as_a_completed_move(
            final BiFunction<ProductFlowsInitializer, Path, FileWritingMessageHandler> handlerFactory, @TempDir final Path workDir) throws Exception {
        final String previousContent = "previous message";
        final Path source = workDir.resolve(SOURCE_FILENAME);
        final Message<File> previousMessage = messageFor(source, FileProcessingIdentifier.newInstance());
        final Path previousDestination = Files.writeString(workDir.resolve(fileNameGenerator.generateFileName(previousMessage)), previousContent);
        final Message<File> message = messageFor(source, FileProcessingIdentifier.newInstance());
        final FileWritingMessageHandler handler = createHandler(handlerFactory, workDir);
        assertThatThrownBy(() -> handler.handleMessage(message))
                .hasRootCauseInstanceOf(NoSuchFileException.class);
        assertThat(previousDestination).hasContent(previousContent);
    }

    @ParameterizedTest
    @MethodSource("handlerFactoriesAndModificationTimes")
    void retrying_a_completed_move_preserves_a_new_input_file_with_the_same_name(
            final BiFunction<ProductFlowsInitializer, Path, FileWritingMessageHandler> handlerFactory,
            final FileTime newFileModified, @TempDir final Path workDir) throws Exception {
        final String originalContent = "original message";
        final String newContent = "new message";
        final Path source = Files.writeString(workDir.resolve(SOURCE_FILENAME), originalContent);
        Files.setLastModifiedTime(source, FILE_MODIFIED);
        final FileProcessingIdentifier processingIdentifier = FileProcessingIdentifier.newInstance();
        final Message<File> message = messageFor(source, processingIdentifier);
        final Path destination = workDir.resolve(fileNameGenerator.generateFileName(message));
        final FileWritingMessageHandler handler = createHandler(handlerFactory, workDir);
        handler.handleMessage(message);
        assertThat(source).doesNotExist();
        assertThat(destination).hasContent(originalContent);

        Files.writeString(source, newContent);
        Files.setLastModifiedTime(source, newFileModified);
        handler.handleMessage(message);

        assertThat(source).hasContent(newContent);
        assertThat(Files.getLastModifiedTime(source)).isEqualTo(newFileModified);
        assertThat(destination).hasContent(originalContent);
    }
}
