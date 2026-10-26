package fi.fmi.avi.archiver.config;

import fi.fmi.avi.archiver.ProcessingState;
import fi.fmi.avi.archiver.file.FileProcessingIdentifier;
import fi.fmi.avi.archiver.spring.context.CompoundLifecycle;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.integration.channel.NullChannel;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.file.FileWritingMessageHandler;
import org.springframework.integration.handler.advice.RequestHandlerRetryAdvice;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.util.ReflectionTestUtils;

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

import static fi.fmi.avi.archiver.config.IntegrationFlowConfig.*;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;

class ArchiveFileRecoveryTest {
    private static final FileTime FILE_MODIFIED = FileTime.from(Instant.parse("2026-10-16T20:53:00Z"));

    private @Nullable FileWritingMessageHandler handler;

    private static Message<File> messageFor(final Path source, final FileProcessingIdentifier processingIdentifier) {
        return MessageBuilder.withPayload(source.toFile())
                .setHeader(FILENAME.getName(), source.getFileName().toString())
                .setHeader(ORIGINAL_FILE.getName(), source.toFile())
                .setHeader(PROCESSING_IDENTIFIER.getName(), processingIdentifier)
                .build();
    }

    private FileWritingMessageHandler createHandler(final String handlerFactory, final Path destinationDir) {
        final IntegrationFlowConfig config = new IntegrationFlowConfig();
        final RequestHandlerRetryAdvice retryAdvice = new RequestHandlerRetryAdvice();
        // Bound retries only in the test, so the regression fails instead of hanging indefinitely.
        retryAdvice.setRetryTemplate(RetryTemplate.builder().maxAttempts(3).noBackoff().build());
        final NullChannel unusedChannel = new NullChannel();
        final IntegrationFlowConfig.ProductFlowsInitializer initializer = new IntegrationFlowConfig.ProductFlowsInitializer(
                mock(IntegrationFlowContext.class), Map.of(), new CompoundLifecycle(), new ProcessingState(Clock.systemUTC()),
                List.of(retryAdvice), List.of(retryAdvice), config.fileProcessingIdAppender(), config.headerToFileTransformer(),
                Duration.ofSeconds(15), unusedChannel, unusedChannel, unusedChannel, unusedChannel, unusedChannel, unusedChannel);
        // Use the application's factory so real behaviour is being tested
        final FileWritingMessageHandler handler = requireNonNull(ReflectionTestUtils.invokeMethod(initializer, handlerFactory, destinationDir));
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
    @ValueSource(strings = {"createArchiveHandler", "createFailHandler"})
    void retrying_a_completed_move_succeeds_when_the_source_is_missing(final String handlerFactory, @TempDir final Path workDir) throws Exception {
        final String filename = "weather.aviation.metar-ESNV-20261016T205000Z.xml";
        final String content = "<METAR>test message</METAR>";
        final Path source = Files.writeString(workDir.resolve(filename), content);
        Files.setLastModifiedTime(source, FILE_MODIFIED);
        final Path destinationDir = Files.createDirectory(workDir.resolve("destination"));
        final FileProcessingIdentifier processingIdentifier = FileProcessingIdentifier.newInstance();
        final Message<File> message = messageFor(source, processingIdentifier);
        final Path destination = destinationDir.resolve(filename + "." + processingIdentifier);
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
    @ValueSource(strings = {"createArchiveHandler", "createFailHandler"})
    void missing_source_and_destination_are_not_reported_as_a_completed_move(final String handlerFactory, @TempDir final Path workDir) {
        final Path source = workDir.resolve("missing.xml");
        final Message<File> message = messageFor(source, FileProcessingIdentifier.newInstance());
        final FileWritingMessageHandler handler = createHandler(handlerFactory, workDir);
        assertThatThrownBy(() -> handler.handleMessage(message))
                .hasRootCauseInstanceOf(NoSuchFileException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"createArchiveHandler", "createFailHandler"})
    void a_temporary_file_does_not_count_as_a_completed_move(final String handlerFactory, @TempDir final Path workDir) throws Exception {
        final Path source = workDir.resolve("missing.xml");
        final FileProcessingIdentifier processingIdentifier = FileProcessingIdentifier.newInstance();
        final Message<File> message = messageFor(source, processingIdentifier);
        final Path temporaryFile = Files.writeString(workDir.resolve("missing.xml." + processingIdentifier + ".writing"), "partial content");
        final FileWritingMessageHandler handler = createHandler(handlerFactory, workDir);
        assertThatThrownBy(() -> handler.handleMessage(message))
                .hasRootCauseInstanceOf(NoSuchFileException.class);
        assertThat(workDir.resolve("missing.xml." + processingIdentifier)).doesNotExist();
        assertThat(temporaryFile).hasContent("partial content");
    }

    @ParameterizedTest
    @ValueSource(strings = {"createArchiveHandler", "createFailHandler"})
    void a_file_from_another_processing_attempt_does_not_count_as_a_completed_move(final String handlerFactory, @TempDir final Path workDir) throws Exception {
        final Path source = workDir.resolve("missing.xml");
        final Path previousDestination = Files.writeString(workDir.resolve("missing.xml." + FileProcessingIdentifier.newInstance()), "previous message");
        final Message<File> message = messageFor(source, FileProcessingIdentifier.newInstance());
        final FileWritingMessageHandler handler = createHandler(handlerFactory, workDir);
        assertThatThrownBy(() -> handler.handleMessage(message))
                .hasRootCauseInstanceOf(NoSuchFileException.class);
        assertThat(previousDestination).hasContent("previous message");
    }

    @ParameterizedTest
    @ValueSource(strings = {"createArchiveHandler", "createFailHandler"})
    void retrying_a_completed_move_preserves_a_new_input_file_with_the_same_name(final String handlerFactory, @TempDir final Path workDir) throws Exception {
        final Path source = Files.writeString(workDir.resolve("message.xml"), "original message");
        Files.setLastModifiedTime(source, FILE_MODIFIED);
        final FileProcessingIdentifier processingIdentifier = FileProcessingIdentifier.newInstance();
        final Message<File> message = messageFor(source, processingIdentifier);
        final Path destination = workDir.resolve("message.xml." + processingIdentifier);
        final FileWritingMessageHandler handler = createHandler(handlerFactory, workDir);
        handler.handleMessage(message);
        assertThat(source).doesNotExist();
        assertThat(destination).hasContent("original message");

        Files.writeString(source, "new message");
        handler.handleMessage(message);

        assertThat(source).hasContent("new message");
        assertThat(destination).hasContent("original message");
    }
}
