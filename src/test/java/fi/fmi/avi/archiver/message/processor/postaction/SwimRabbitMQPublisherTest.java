package fi.fmi.avi.archiver.message.processor.postaction;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteStreams;
import com.google.common.io.Resources;
import com.google.common.util.concurrent.MoreExecutors;
import com.rabbitmq.client.amqp.Message;
import com.rabbitmq.client.amqp.Publisher;
import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchivalStatus;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.ProcessingResult;
import fi.fmi.avi.archiver.message.processor.MessageProcessorContext;
import fi.fmi.avi.archiver.message.processor.TestMessageProcessorContext;
import fi.fmi.avi.model.AviationWeatherMessage;
import fi.fmi.avi.model.MessageType;
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
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SwimRabbitMQPublisherTest {
    public static final Consumer<Publisher.Context> DUMMY_CONTEXT_CONSUMER = context -> {
    };
    private static final String INSTANCE_ID = "test";
    private static final String STATION_ICAO_CODE = "EFHK";

    private static final int FORMAT_TAC = 1;
    private static final int FORMAT_IWXXM = 2;

    private static final int MESSAGE_TYPE_METAR = 1;
    private static final int MESSAGE_TYPE_SPECI = 2;
    private static final int MESSAGE_TYPE_TAF = 3;
    private static final int MESSAGE_TYPE_SIGMET = 4;
    private static final int MESSAGE_TYPE_UNSUPPORTED = 17;

    private static final String METAR_SPEC = "https://eur-registry.swim.aero/services/eurocontrol-iwxxm-metar-speci-subscription-and-request-service-10";
    private static final String TAF_SPEC = "https://eur-registry.swim.aero/services/eurocontrol-iwxxm-taf-subscription-and-request-service-10";
    private static final String SIGMET_SPEC = "https://eur-registry.swim.aero/services/eurocontrol-iwxxm-sigmet-subscription-and-request-service-10";

    private static final Map<Integer, SwimRabbitMQPublisher.StaticApplicationProperties> APPLICATION_PROPERTIES_BY_MESSAGE_TYPE =
            Map.of(
                    MESSAGE_TYPE_METAR, new SwimRabbitMQPublisher.StaticApplicationProperties(MessageType.METAR, "weather.aviation.metar", METAR_SPEC, "AD"),
                    MESSAGE_TYPE_SPECI, new SwimRabbitMQPublisher.StaticApplicationProperties(MessageType.SPECI, "weather.aviation.metar", METAR_SPEC, "AD"),
                    MESSAGE_TYPE_TAF, new SwimRabbitMQPublisher.StaticApplicationProperties(MessageType.TAF, "weather.aviation.taf", TAF_SPEC, "AD"),
                    MESSAGE_TYPE_SIGMET, new SwimRabbitMQPublisher.StaticApplicationProperties(MessageType.SIGMET, "weather.aviation.sigmet", SIGMET_SPEC, "FIR")
            );

    private static final String KEY_REPORT_STATUS = "properties.report_status";
    private static final String KEY_ICAO_LOCATION_IDENTIFIER = "properties.icao_location_identifier";
    private static final String KEY_ISSUE_DATETIME = "properties.issue_datetime";
    private static final String KEY_ICAO_LOCATION_TYPE = "properties.icao_location_type";
    private static final String KEY_CONFORMS_TO = "conformsTo";
    private static final String KEY_OBSERVATION_DATETIME = "properties.datetime";
    private static final String KEY_START_DATETIME = "properties.start_datetime";
    private static final String KEY_END_DATETIME = "properties.end_datetime";
    private static final Instant NOW = Instant.parse("2025-08-26T09:12:56.472Z");

    @Mock
    private Publisher publisher;
    @Mock(answer = Answers.RETURNS_SELF)
    private Message amqpMessage;
    @Mock
    private Publisher.Context publisherContext;
    private AutoCloseable openMocks;

    static Stream<Arguments> ignores_message_cases() {
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

    static Stream<AppPropScenario> app_property_scenarios() {
        return Stream.of(
                // METAR
                new AppPropScenario(
                        MESSAGE_TYPE_METAR,
                        "metar-message.xml",
                        "",
                        SwimRabbitMQPublisher.ContentEncoding.IDENTITY,
                        AviationWeatherMessage.ReportStatus.NORMAL,
                        4,
                        NOW.minusSeconds(300).atOffset(ZoneOffset.UTC),
                        null,
                        null,
                        Map.of(KEY_OBSERVATION_DATETIME, NOW.minusSeconds(300).toString()),
                        Set.of(KEY_START_DATETIME, KEY_END_DATETIME),
                        "weather.aviation.metar",
                        "AD",
                        null
                ),
                // METAR without observation time
                new AppPropScenario(
                        MESSAGE_TYPE_METAR,
                        "metar-message.xml",
                        "",
                        SwimRabbitMQPublisher.ContentEncoding.IDENTITY,
                        AviationWeatherMessage.ReportStatus.NORMAL,
                        4,
                        null,
                        null,
                        null,
                        Map.of(),
                        Set.of(KEY_OBSERVATION_DATETIME, KEY_START_DATETIME, KEY_END_DATETIME),
                        "weather.aviation.metar",
                        "AD",
                        IllegalArgumentException.class
                ),
                // SPECI
                new AppPropScenario(
                        MESSAGE_TYPE_SPECI,
                        "speci-message.xml",
                        "",
                        SwimRabbitMQPublisher.ContentEncoding.IDENTITY,
                        AviationWeatherMessage.ReportStatus.NORMAL,
                        7,
                        NOW.minusSeconds(120).atOffset(ZoneOffset.UTC),
                        null,
                        null,
                        Map.of(KEY_OBSERVATION_DATETIME, NOW.minusSeconds(120).toString()),
                        Set.of(KEY_START_DATETIME, KEY_END_DATETIME),
                        "weather.aviation.metar",
                        "AD",
                        null
                ),
                // TAF without valid time
                new AppPropScenario(
                        MESSAGE_TYPE_TAF,
                        "taf-message.xml",
                        "",
                        SwimRabbitMQPublisher.ContentEncoding.GZIP,
                        AviationWeatherMessage.ReportStatus.NORMAL,
                        5,
                        null,
                        null,
                        null,
                        Map.of(),
                        Set.of(KEY_OBSERVATION_DATETIME, KEY_START_DATETIME, KEY_END_DATETIME),
                        "weather.aviation.taf",
                        "AD",
                        IllegalArgumentException.class
                ),
                // AMD TAF
                new AppPropScenario(
                        MESSAGE_TYPE_TAF,
                        "taf-message.xml",
                        "AAC",
                        SwimRabbitMQPublisher.ContentEncoding.GZIP,
                        AviationWeatherMessage.ReportStatus.AMENDMENT,
                        6,
                        null,
                        NOW.plusSeconds(3600).atOffset(ZoneOffset.UTC),
                        NOW.plusSeconds(3600 * 6).atOffset(ZoneOffset.UTC),
                        Map.of(
                                KEY_START_DATETIME, NOW.plusSeconds(3600).toString(),
                                KEY_END_DATETIME, NOW.plusSeconds(3600 * 6).toString()
                        ),
                        Set.of(KEY_OBSERVATION_DATETIME),
                        "weather.aviation.taf",
                        "AD",
                        null
                ),
                // SIGMET
                new AppPropScenario(
                        MESSAGE_TYPE_SIGMET,
                        "sigmet-message.xml",
                        "",
                        SwimRabbitMQPublisher.ContentEncoding.IDENTITY,
                        AviationWeatherMessage.ReportStatus.NORMAL,
                        0,
                        null,
                        NOW.minusSeconds(600).atOffset(ZoneOffset.UTC),
                        NOW.plusSeconds(1800).atOffset(ZoneOffset.UTC),
                        Map.of(
                                KEY_START_DATETIME, NOW.minusSeconds(600).toString(),
                                KEY_END_DATETIME, NOW.plusSeconds(1800).toString()
                        ),
                        Set.of(KEY_OBSERVATION_DATETIME),
                        "weather.aviation.sigmet",
                        "FIR",
                        null
                ),
                // SIGMET without valid end
                new AppPropScenario(
                        MESSAGE_TYPE_SIGMET,
                        "sigmet-message.xml",
                        "",
                        SwimRabbitMQPublisher.ContentEncoding.IDENTITY,
                        AviationWeatherMessage.ReportStatus.NORMAL,
                        0,
                        null,
                        NOW.minusSeconds(900).atOffset(ZoneOffset.UTC),
                        null,
                        Map.of(KEY_START_DATETIME, NOW.minusSeconds(900).toString()),
                        Set.of(KEY_OBSERVATION_DATETIME, KEY_END_DATETIME),
                        "weather.aviation.sigmet",
                        "FIR",
                        IllegalArgumentException.class
                )
        );
    }

    private static String readResource(final String filename) throws IOException {
        return Resources.toString(requireNonNull(SwimRabbitMQPublisherTest.class.getResource(filename)), StandardCharsets.UTF_8);
    }

    private static ArchiveAviationMessage newMessage(final int type, final String content, final Instant messageTime) {
        return ArchiveAviationMessage.builder()
                .setProcessingResult(ProcessingResult.OK)
                .setArchivalStatus(ArchivalStatus.ARCHIVED)
                .setFormat(FORMAT_IWXXM)
                .setType(type)
                .setStationIcaoCode(STATION_ICAO_CODE)
                .setMessageTime(messageTime)
                .setValidFrom(messageTime)
                .setValidTo(messageTime.plusSeconds(1800))
                .setMessage(content)
                .buildPartial();
    }

    private static AbstractRetryingPostAction.RetryParams retryParams(final int maxAttempts) {
        final RetryTemplate retryTemplate = new RetryTemplate();
        retryTemplate.setRetryPolicy(new SimpleRetryPolicy(maxAttempts));
        return new AbstractRetryingPostAction.RetryParams(MoreExecutors.newDirectExecutorService(), Duration.ofSeconds(30), retryTemplate);
    }

    private static SwimRabbitMQPublisher.MessageConfig createMessageConfig(
            final SwimRabbitMQPublisher.ContentEncoding encoding, final Duration expiryTime) {
        return SwimRabbitMQPublisher.MessageConfig.builder()
                .addPriorities(
                        SwimRabbitMQPublisher.MessageConfig.PriorityDescriptor.builder()
                                .setMessageType(MESSAGE_TYPE_SPECI)
                                .setPriority(7),
                        SwimRabbitMQPublisher.MessageConfig.PriorityDescriptor.builder()
                                .setReportStatus(AviationWeatherMessage.ReportStatus.AMENDMENT)
                                .setMessageType(MESSAGE_TYPE_TAF)
                                .setPriority(6),
                        SwimRabbitMQPublisher.MessageConfig.PriorityDescriptor.builder()
                                .setMessageType(MESSAGE_TYPE_TAF)
                                .setPriority(5),
                        SwimRabbitMQPublisher.MessageConfig.PriorityDescriptor.builder()
                                .setMessageType(MESSAGE_TYPE_METAR)
                                .setPriority(4),
                        SwimRabbitMQPublisher.MessageConfig.PriorityDescriptor.builder()
                                .setPriority(0)
                )
                .setEncoding(encoding)
                .setExpiryTime(expiryTime)
                .build();
    }

    private static ArchiveAviationMessage createArchiveAviationMessage(final AppPropScenario scenario, final String messageContent) {
        final ArchiveAviationMessage.Builder msgBuilder = ArchiveAviationMessage.builder()
                .setProcessingResult(ProcessingResult.OK)
                .setArchivalStatus(ArchivalStatus.ARCHIVED)
                .setFormat(FORMAT_IWXXM)
                .setType(scenario.messageType())
                .setStationIcaoCode(STATION_ICAO_CODE)
                .setVersion(scenario.version())
                .setMessageTime(NOW)
                .setMessage(messageContent);
        if (scenario.validFrom() != null) {
            msgBuilder.setValidFrom(scenario.validFrom().toInstant());
        }
        if (scenario.validTo() != null) {
            msgBuilder.setValidTo(scenario.validTo().toInstant());
        }
        return msgBuilder.buildPartial();
    }

    private static MessageProcessorContext newContext() {
        return TestMessageProcessorContext.create(InputAviationMessage.builder().buildPartial());
    }

    @BeforeEach
    void setUp() {
        openMocks = MockitoAnnotations.openMocks(this);
        when(publisher.message(any())).thenReturn(amqpMessage);
        when(publisherContext.status()).thenReturn(Publisher.Status.ACCEPTED);
        when(publisherContext.failureCause()).thenReturn(null);
        doAnswer(invocation -> {
            final Publisher.Callback callback = invocation.getArgument(1);
            callback.handle(publisherContext);
            return null;
        }).when(publisher).publish(any(Message.class), any(Publisher.Callback.class));
    }

    @AfterEach
    void tearDown() throws Exception {
        if (openMocks != null) {
            openMocks.close();
        }
    }

    @ParameterizedTest
    @MethodSource("ignores_message_cases")
    void test_ignores_message(final String ignoredDescription, final ArchiveAviationMessage archiveAviationMessage) throws Exception {
        try (final SwimRabbitMQPublisher publisher = newPublisher(Clock.systemUTC(), SwimRabbitMQPublisher.MessageConfig.builder().buildPartial())) {
            publisher.run(newContext(), archiveAviationMessage);
        }
        verifyNoInteractions(this.publisher);
    }

    @ParameterizedTest
    @EnumSource(SwimRabbitMQPublisher.ContentEncoding.class)
    void test_content_encoding(final SwimRabbitMQPublisher.ContentEncoding encoding) throws Exception {
        final String content = readResource("taf-message.xml");
        try (final SwimRabbitMQPublisher publisher = newPublisher(Clock.systemUTC(),
                SwimRabbitMQPublisher.MessageConfig.builder().setEncoding(encoding).build())) {
            publisher.run(newContext(), newMessage(MESSAGE_TYPE_TAF, content, NOW));
        }

        verify(amqpMessage).contentEncoding(encoding.value());
        final byte[] encoded = capturePublishedBody();
        final String decoded = ContentEncodingTests.valueOf(encoding).decode(encoded);
        if (encoding == SwimRabbitMQPublisher.ContentEncoding.IDENTITY) {
            assertThat(encoded).isEqualTo(content.getBytes(StandardCharsets.UTF_8));
        } else {
            assertThat(encoded).isNotEqualTo(content.getBytes(StandardCharsets.UTF_8));
        }
        assertThat(decoded).isEqualTo(content);
    }

    @ParameterizedTest
    @MethodSource("app_property_scenarios")
    void test_application_property_scenarios(final AppPropScenario scenario) throws Exception {
        when(amqpMessage.creationTime()).thenReturn(NOW.toEpochMilli());

        final String content = readResource(scenario.resourceFile());
        final Duration expiryTime = Duration.ofHours(12);
        final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        final SwimRabbitMQPublisher.MessageConfig config = createMessageConfig(scenario.encoding(), expiryTime);
        final ArchiveAviationMessage archiveAviationMessage = createArchiveAviationMessage(scenario, content);

        final InputAviationMessage.Builder inputBuilder = InputAviationMessage.builder();
        if (scenario.observationTime() != null) {
            inputBuilder.setIwxxmObservationTime(scenario.observationTime());
        }
        final MessageProcessorContext context = TestMessageProcessorContext.create(inputBuilder.buildPartial());

        if (scenario.expectedException() != null) {
            try (final SwimRabbitMQPublisher publisher = newPublisher(clock, config)) {
                assertThrows(scenario.expectedException(), () -> publisher.runAsynchronously(context, archiveAviationMessage));
            }
            verify(publisher, never()).publish(any(Message.class), any(Publisher.Callback.class));
            return;
        }

        try (final SwimRabbitMQPublisher publisher = newPublisher(clock, config)) {
            publisher.run(context, archiveAviationMessage);
        }

        verifyScenario(scenario, NOW, expiryTime, scenario.encoding());
    }

    @Test
    void test_retries_until_success() throws Exception {
        final int succeedOn = 3;

        final String content = readResource("taf-message.xml");
        final TestPublisherContext failing1 = new TestPublisherContext(amqpMessage, Publisher.Status.REJECTED, new RuntimeException("fail-1"));
        final TestPublisherContext failing2 = new TestPublisherContext(amqpMessage, Publisher.Status.REJECTED, new RuntimeException("fail-2"));
        final TestPublisherContext success = new TestPublisherContext(amqpMessage, Publisher.Status.ACCEPTED, null);

        final AtomicInteger attempts = new AtomicInteger();
        when(publisher.message(any(byte[].class))).thenReturn(amqpMessage);
        doAnswer(invocation -> {
            final Publisher.Callback callback = invocation.getArgument(1);
            callback.handle(switch (attempts.incrementAndGet()) {
                case 1 -> failing1;
                case 2 -> failing2;
                default -> success;
            });
            return null;
        }).when(publisher).publish(any(Message.class), any(Publisher.Callback.class));

        try (final SwimRabbitMQPublisher publisher = newPublisher(Clock.systemUTC(), SwimRabbitMQPublisher.MessageConfig.builder().build())) {
            publisher.run(newContext(), newMessage(MESSAGE_TYPE_TAF, content, NOW));
        }

        verify(publisher, times(succeedOn)).publish(any(Message.class), any(Publisher.Callback.class));
        assertThat(attempts.get()).isEqualTo(succeedOn);
    }

    @Test
    void test_retries_exhausted() throws Exception {
        final int maxAttempts = 4;
        final AbstractRetryingPostAction.RetryParams retryParams = retryParams(maxAttempts);
        final String content = readResource("taf-message.xml");

        when(publisher.message(any(byte[].class))).thenReturn(amqpMessage);
        final AtomicInteger attempt = new AtomicInteger();
        doAnswer(inv -> {
            final Publisher.Callback cb = inv.getArgument(1);
            cb.handle(new TestPublisherContext(amqpMessage, Publisher.Status.REJECTED,
                    new RuntimeException("attempt-" + attempt.incrementAndGet())));
            return null;
        }).when(publisher).publish(any(Message.class), any(Publisher.Callback.class));

        try (final SwimRabbitMQPublisher publisher = newPublisher(Clock.systemUTC(), SwimRabbitMQPublisher.MessageConfig.builder().build(), retryParams)) {
            publisher.run(newContext(), newMessage(MESSAGE_TYPE_TAF, content, NOW));
        }

        verify(publisher, times(maxAttempts)).publish(any(Message.class), any(Publisher.Callback.class));
    }

    private SwimRabbitMQPublisher newPublisher(final Clock clock, final SwimRabbitMQPublisher.MessageConfig config) {
        return newPublisher(clock, config, retryParams(10));
    }

    private SwimRabbitMQPublisher newPublisher(final Clock clock, final SwimRabbitMQPublisher.MessageConfig config,
                                               final AbstractRetryingPostAction.RetryParams retryParams) {
        return new SwimRabbitMQPublisher(
                retryParams,
                INSTANCE_ID,
                publisher,
                DUMMY_CONTEXT_CONSUMER,
                clock,
                FORMAT_IWXXM,
                APPLICATION_PROPERTIES_BY_MESSAGE_TYPE,
                config);
    }

    private void verifyScenario(final AppPropScenario scenario,
                                final Instant creation,
                                final Duration expiryTime,
                                final SwimRabbitMQPublisher.ContentEncoding encoding) {
        verifyProperties(scenario.expectedSubject(), creation, encoding, scenario.expectedPriority(), expiryTime);

        final ImmutableMap.Builder<String, String> expectedBuilder = ImmutableMap.builder();
        expectedBuilder.put(KEY_REPORT_STATUS, scenario.expectedReportStatus().toString());
        expectedBuilder.put(KEY_ICAO_LOCATION_IDENTIFIER, STATION_ICAO_CODE);
        expectedBuilder.put(KEY_ISSUE_DATETIME, creation.toString());
        expectedBuilder.put(KEY_ICAO_LOCATION_TYPE, scenario.expectedIcaoLocationType());
        expectedBuilder.put(KEY_CONFORMS_TO, APPLICATION_PROPERTIES_BY_MESSAGE_TYPE.get(scenario.messageType()).conformsTo());
        expectedBuilder.putAll(scenario.expectedOptionalProps());
        final Map<String, String> expected = expectedBuilder.build();

        expected.forEach((key, value) -> verify(amqpMessage).property(key, value));
        scenario.expectedAbsentProps().forEach(key -> verify(amqpMessage, never()).property(eq(key), anyString()));

        assertPublishedSameMessageInstance();
    }

    private void verifyProperties(final String subject,
                                  final Instant creation,
                                  final SwimRabbitMQPublisher.ContentEncoding encoding,
                                  final int priority,
                                  final Duration expiryTime) {
        verify(amqpMessage).messageId(any(UUID.class));
        verify(amqpMessage).contentType("application/xml");
        verify(amqpMessage).contentEncoding(encoding.value());
        verify(amqpMessage).priority((byte) priority);
        verify(amqpMessage).subject(subject);
        verify(amqpMessage).creationTime(creation.toEpochMilli());
        verify(amqpMessage, atLeastOnce()).creationTime();
        verify(amqpMessage).absoluteExpiryTime(creation.toEpochMilli() + expiryTime.toMillis());
    }

    private byte[] capturePublishedBody() {
        final ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
        verify(publisher).message(captor.capture());
        return captor.getValue();
    }

    private void assertPublishedSameMessageInstance() {
        final ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(publisher).publish(captor.capture(), any());
        assertThat(captor.getValue()).isSameAs(amqpMessage);
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
                    return new String(ByteStreams.toByteArray(gzipInputStream), StandardCharsets.UTF_8);
                }
            }
        };

        static ContentEncodingTests valueOf(final SwimRabbitMQPublisher.ContentEncoding encoding) {
            return valueOf(encoding.name());
        }

        abstract String decode(final byte[] encodedContent) throws IOException;
    }

    private record AppPropScenario(
            int messageType,
            String resourceFile,
            String version,
            SwimRabbitMQPublisher.ContentEncoding encoding,
            AviationWeatherMessage.ReportStatus expectedReportStatus,
            int expectedPriority,
            @Nullable OffsetDateTime observationTime,
            @Nullable OffsetDateTime validFrom,
            @Nullable OffsetDateTime validTo,
            Map<String, String> expectedOptionalProps,
            Set<String> expectedAbsentProps,
            String expectedSubject,
            String expectedIcaoLocationType,
            @Nullable Class<? extends Throwable> expectedException
    ) {
    }

    record TestPublisherContext(Message message, Publisher.Status status,
                                @Nullable Throwable failureCause) implements Publisher.Context {
    }
}