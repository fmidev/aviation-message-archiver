package fi.fmi.avi.archiver.message.processor.postaction;

import com.google.common.io.ByteStreams;
import com.google.common.io.Resources;
import com.rabbitmq.client.amqp.Message;
import com.rabbitmq.client.amqp.Publisher;
import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchivalStatus;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.ProcessingResult;
import fi.fmi.avi.archiver.message.processor.MessageProcessorContext;
import fi.fmi.avi.archiver.message.processor.TestMessageProcessorContext;
import fi.fmi.avi.model.AviationWeatherMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SwimRabbitMQPublisherTest {
    public static final Consumer<Publisher.Context> DUMMY_CONTEXT_CONSUMER = context -> {
    };
    private static final int FORMAT_TAC = 1;
    private static final int FORMAT_IWXXM = 2;
    private static final int MESSAGE_TYPE_METAR = 1;
    private static final int MESSAGE_TYPE_SPECI = 2;
    private static final int MESSAGE_TYPE_TAF = 3;
    private static final int MESSAGE_TYPE_SIGMET = 4;
    private static final int MESSAGE_TYPE_UNSUPPORTED = 17;
    private static final Map<Integer, String> SUBJECT_BY_MESSAGE_TYPE_ID = Map.of(
            MESSAGE_TYPE_METAR, "weather.aviation.metar",
            MESSAGE_TYPE_SPECI, "weather.aviation.metar",
            MESSAGE_TYPE_TAF, "weather.aviation.taf",
            MESSAGE_TYPE_SIGMET, "weather.aviation.sigmet"
    );
    @Mock
    private Publisher publisher;
    @Mock(answer = Answers.RETURNS_SELF)
    private Message amqpMessage;

    private AutoCloseable openMocks;

    private static String readResourceToString(final String filename) throws IOException {
        return Resources.toString(requireNonNull(SwimRabbitMQPublisherTest.class.getResource(filename)), StandardCharsets.UTF_8);
    }

    private static byte[] readResourceToByteArray(final String filename) throws IOException {
        return Resources.toByteArray(requireNonNull(SwimRabbitMQPublisherTest.class.getResource(filename)));
    }

    private static ArchiveAviationMessage newArchiveAviationMessage(final String content) {
        return ArchiveAviationMessage.builder()
                .setProcessingResult(ProcessingResult.OK)
                .setArchivalStatus(ArchivalStatus.ARCHIVED)
                .setFormat(FORMAT_IWXXM)
                .setType(MESSAGE_TYPE_TAF)
                .setMessage(content)
                .buildPartial();
    }

    static Stream<Arguments> test_ignores_message() {
        return Stream.of(
                arguments("Not IWXXM", ArchiveAviationMessage.builder()
                        .setProcessingResult(ProcessingResult.OK)
                        .setArchivalStatus(ArchivalStatus.ARCHIVED)
                        .setFormat(FORMAT_TAC)
                        .setType(MESSAGE_TYPE_TAF)
                        .buildPartial()),
                arguments("Unsupported type", ArchiveAviationMessage.builder()
                        .setProcessingResult(ProcessingResult.OK)
                        .setArchivalStatus(ArchivalStatus.ARCHIVED)
                        .setFormat(FORMAT_IWXXM)
                        .setType(MESSAGE_TYPE_UNSUPPORTED)
                        .buildPartial())
        );
    }

    @BeforeEach
    void setUp() {
        openMocks = MockitoAnnotations.openMocks(this);
        when(publisher.message(any())).thenReturn(amqpMessage);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (openMocks != null) {
            openMocks.close();
        }
    }

    @ParameterizedTest
    @MethodSource
    void test_ignores_message(final String ignoredDescription, final ArchiveAviationMessage archiveAviationMessage) {
        final InputAviationMessage inputAviationMessage = InputAviationMessage.builder().buildPartial();
        final MessageProcessorContext messageProcessorContext = TestMessageProcessorContext.create(inputAviationMessage);
        final SwimRabbitMQPublisher.MessageConfig messageConfig = SwimRabbitMQPublisher.MessageConfig.builder().buildPartial();

        final SwimRabbitMQPublisher swimRabbitMQPublisher = new SwimRabbitMQPublisher(
                publisher, DUMMY_CONTEXT_CONSUMER, Clock.systemUTC(), FORMAT_IWXXM, SUBJECT_BY_MESSAGE_TYPE_ID, messageConfig);

        swimRabbitMQPublisher.run(messageProcessorContext, archiveAviationMessage);

        verifyNoInteractions(publisher);
    }

    @ParameterizedTest
    @EnumSource(SwimRabbitMQPublisher.ContentEncoding.class)
    void test_content_encoding(final SwimRabbitMQPublisher.ContentEncoding encoding) throws IOException {
        final InputAviationMessage inputAviationMessage = InputAviationMessage.builder().buildPartial();
        final MessageProcessorContext messageProcessorContext = TestMessageProcessorContext.create(inputAviationMessage);
        final String content = readResourceToString("taf-message.xml");
        final ArchiveAviationMessage archiveAviationMessage = newArchiveAviationMessage(content);
        final SwimRabbitMQPublisher.MessageConfig messageConfig = SwimRabbitMQPublisher.MessageConfig.builder()
                .setEncoding(encoding)
                .build();

        final SwimRabbitMQPublisher swimRabbitMQPublisher = new SwimRabbitMQPublisher(
                publisher, DUMMY_CONTEXT_CONSUMER, Clock.systemUTC(), FORMAT_IWXXM, SUBJECT_BY_MESSAGE_TYPE_ID, messageConfig);

        swimRabbitMQPublisher.run(messageProcessorContext, archiveAviationMessage);

        verify(amqpMessage).contentEncoding(encoding.value());
        final ArgumentCaptor<byte[]> contentCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(publisher).message(contentCaptor.capture());
        final byte[] encodedContent = contentCaptor.getValue();
        if (encoding == SwimRabbitMQPublisher.ContentEncoding.IDENTITY) {
            assertThat(encodedContent).isEqualTo(content.getBytes(StandardCharsets.UTF_8));
        } else {
            assertThat(encodedContent).isNotEqualTo(content.getBytes(StandardCharsets.UTF_8));
        }
        final String decodedContent = ContentEncodingTests.valueOf(encoding).decode(encodedContent);
        assertThat(decodedContent).isEqualTo(content);
    }

    @Test
    void test_mandatory_properties() throws IOException {
        final InputAviationMessage inputAviationMessage = InputAviationMessage.builder().buildPartial();
        final MessageProcessorContext messageProcessorContext = TestMessageProcessorContext.create(inputAviationMessage);
        final String content = readResourceToString("taf-message.xml");
        final ArchiveAviationMessage archiveAviationMessage = newArchiveAviationMessage(content);
        final SwimRabbitMQPublisher.MessageConfig messageConfig = SwimRabbitMQPublisher.MessageConfig.builder()
                .build();
        final Instant now = Instant.parse("2025-08-26T09:12:56.472Z");

        final SwimRabbitMQPublisher swimRabbitMQPublisher = new SwimRabbitMQPublisher(
                publisher, DUMMY_CONTEXT_CONSUMER, Clock.fixed(now, ZoneOffset.ofHours(3)), FORMAT_IWXXM, SUBJECT_BY_MESSAGE_TYPE_ID, messageConfig);

        swimRabbitMQPublisher.run(messageProcessorContext, archiveAviationMessage);

        verify(amqpMessage).messageId(any(UUID.class));
        verify(amqpMessage).contentType("application/xml");
        verify(amqpMessage).contentEncoding("identity");
        verify(amqpMessage).priority((byte) 0);
        verify(amqpMessage).subject("weather.aviation.taf");
        verify(amqpMessage).creationTime(now.toEpochMilli());
        verify(amqpMessage, atLeastOnce()).creationTime();
        verifyNoMoreInteractions(amqpMessage);

        final ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(publisher).publish(messageCaptor.capture(), any());
        final Message actualMessage = messageCaptor.getValue();
        assertThat(actualMessage).isSameAs(amqpMessage);
    }

    @Test
    void test_optional_properties() throws IOException {
        final InputAviationMessage inputAviationMessage = InputAviationMessage.builder().buildPartial();
        final MessageProcessorContext messageProcessorContext = TestMessageProcessorContext.create(inputAviationMessage);
        final String content = readResourceToString("taf-message.xml");
        final ArchiveAviationMessage archiveAviationMessage = newArchiveAviationMessage(content);
        final Duration expiryTime = Duration.ofHours(12);
        final SwimRabbitMQPublisher.MessageConfig messageConfig = SwimRabbitMQPublisher.MessageConfig.builder()
                .addPriorities(
                        SwimRabbitMQPublisher.MessageConfig.PriorityDescriptor.builder()
                                .setReportStatus(AviationWeatherMessage.ReportStatus.AMENDMENT)
                                .setMessageType(MESSAGE_TYPE_TAF)
                                .setPriority(6),
                        SwimRabbitMQPublisher.MessageConfig.PriorityDescriptor.builder()
                                .setMessageType(MESSAGE_TYPE_TAF)
                                .setPriority(5),
                        SwimRabbitMQPublisher.MessageConfig.PriorityDescriptor.builder()
                                .setPriority(0)
                )
                .setEncoding(SwimRabbitMQPublisher.ContentEncoding.GZIP)
                .setExpiryTime(expiryTime)
                .build();
        final Instant now = Instant.parse("2025-08-26T09:12:56.472Z");
        when(amqpMessage.creationTime()).thenReturn(now.toEpochMilli());

        final SwimRabbitMQPublisher swimRabbitMQPublisher = new SwimRabbitMQPublisher(
                publisher, DUMMY_CONTEXT_CONSUMER, Clock.fixed(now, ZoneOffset.ofHours(3)), FORMAT_IWXXM, SUBJECT_BY_MESSAGE_TYPE_ID, messageConfig);

        swimRabbitMQPublisher.run(messageProcessorContext, archiveAviationMessage);

        verify(amqpMessage).messageId(any(UUID.class));
        verify(amqpMessage).contentType("application/xml");
        verify(amqpMessage).contentEncoding("gzip");
        verify(amqpMessage).priority((byte) 5);
        verify(amqpMessage).subject("weather.aviation.taf");
        verify(amqpMessage).creationTime(now.toEpochMilli());
        verify(amqpMessage, atLeastOnce()).creationTime();
        verify(amqpMessage).absoluteExpiryTime(now.toEpochMilli() + expiryTime.toMillis());
        verifyNoMoreInteractions(amqpMessage);

        final ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(publisher).publish(messageCaptor.capture(), any());
        final Message actualMessage = messageCaptor.getValue();
        assertThat(actualMessage).isSameAs(amqpMessage);
    }

    enum ContentEncodingTests {
        IDENTITY {
            @Override
            String decode(final byte[] encodedContent) {
                return new String(encodedContent, StandardCharsets.UTF_8);
            }
        },
        GZIP {
            @Override
            String decode(final byte[] encodedContent) throws IOException {
                try (final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(encodedContent);
                     final GZIPInputStream gzipInputStream = new GZIPInputStream(byteArrayInputStream)) {
                    final byte[] decodedContent = ByteStreams.toByteArray(gzipInputStream);
                    return new String(decodedContent, StandardCharsets.UTF_8);
                }
            }
        };

        static ContentEncodingTests valueOf(final SwimRabbitMQPublisher.ContentEncoding encoding) {
            return valueOf(encoding.name());
        }

        abstract String decode(final byte[] encodedContent) throws IOException;
    }
}
