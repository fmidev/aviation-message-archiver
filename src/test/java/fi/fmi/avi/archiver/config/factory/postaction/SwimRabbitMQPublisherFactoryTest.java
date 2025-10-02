package fi.fmi.avi.archiver.config.factory.postaction;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.util.concurrent.MoreExecutors;
import com.rabbitmq.client.amqp.*;
import com.rabbitmq.client.amqp.impl.AmqpEnvironmentBuilder;
import fi.fmi.avi.archiver.config.factory.postaction.SwimRabbitMQPublisherFactory.Config;
import fi.fmi.avi.archiver.message.processor.postaction.AbstractRetryingPostAction;
import fi.fmi.avi.archiver.message.processor.postaction.SwimRabbitMQPublisher;
import fi.fmi.avi.archiver.spring.healthcontributor.RabbitMQConnectionHealthIndicator;
import fi.fmi.avi.archiver.spring.healthcontributor.RabbitMQPublisherHealthIndicator;
import fi.fmi.avi.archiver.spring.healthcontributor.SwimRabbitMQConnectionHealthContributor;
import fi.fmi.avi.archiver.util.instantiation.ObjectFactoryConfigFactory;
import fi.fmi.avi.model.AviationWeatherMessage;
import fi.fmi.avi.model.MessageType;
import org.inferred.freebuilder.FreeBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.mockito.stubbing.Answer;
import org.springframework.retry.support.RetryTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SwimRabbitMQPublisherFactoryTest {
    private static final int IWXXM_FORMAT_ID = 2;
    private static final ImmutableBiMap<MessageType, Integer> MESSAGE_TYPE_IDS = ImmutableBiMap.<MessageType, Integer>builder()
            .put(MessageType.METAR, 2)
            .put(MessageType.SPECI, 3)
            .put(MessageType.TAF, 4)
            .put(MessageType.SIGMET, 5)
            .put(MessageType.SPACE_WEATHER_ADVISORY, 6)
            .build();
    private static final TestConfig MINIMAL_CONFIG = TestConfig.builder()
            .id("test-id-minimal")
            .publisherQueueCapacity(50)
            .connection(TestConnectionConfig.builder()
                    .uri("amqp://localhost:5672/%2f")
                    .username("user")
                    .password("pass")
                    .build())
            .topology(TestTopologyConfig.builder()
                    .routingKey("aviation-message-archiver-test-routing1")
                    .exchange(TestExchangeConfig.builder()
                            .name("aviation-message-archiver-test-exchange1")
                            .create(false)
                            .build())
                    .queue(TestQueueConfig.builder()
                            .name("aviation-message-archiver-test-queue1")
                            .create(false)
                            .build())
                    .build())
            .retry(TestRetryConfig.builder()
                    .timeout(Duration.of(2, ChronoUnit.MINUTES))
                    .build())
            .build();
    private static final List<SwimRabbitMQPublisher.MessageConfig.PriorityDescriptor> DEFAULT_PRIORITIES = List.of(
            SwimRabbitMQPublisher.MessageConfig.PriorityDescriptor.builder()
                    .setMessageType(messageTypeId(MessageType.SIGMET))
                    .setPriority(7)
                    .build(),
            SwimRabbitMQPublisher.MessageConfig.PriorityDescriptor.builder()
                    .setMessageType(messageTypeId(MessageType.SPECI))
                    .setPriority(6)
                    .build(),
            SwimRabbitMQPublisher.MessageConfig.PriorityDescriptor.builder()
                    .setMessageType(messageTypeId(MessageType.TAF))
                    .setReportStatus(AviationWeatherMessage.ReportStatus.AMENDMENT)
                    .setPriority(6)
                    .build(),
            SwimRabbitMQPublisher.MessageConfig.PriorityDescriptor.builder()
                    .setMessageType(messageTypeId(MessageType.TAF))
                    .setPriority(5)
                    .build(),
            SwimRabbitMQPublisher.MessageConfig.PriorityDescriptor.builder()
                    .setMessageType(messageTypeId(MessageType.METAR))
                    .setPriority(4)
                    .build()
    );
    private static final SwimRabbitMQPublisher.MessageConfig EXPECTED_MESSAGE_CONFIG = SwimRabbitMQPublisher.MessageConfig.builder()
            .setExchange(MINIMAL_CONFIG.topology().exchange().name())
            .addAllPriorities(DEFAULT_PRIORITIES)
            .build();
    private static final Clock CLOCK = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC);

    private final List<AbstractRetryingPostAction.RetryParams> capturedRetryParams = new ArrayList<>();

    @Mock
    private ObjectFactoryConfigFactory configFactory;
    @Mock
    private RetryingPostActionFactories.RetryParamsFactory retryParamsFactory;
    @Mock
    private RetryTemplate retryTemplate;
    @Mock
    private SwimRabbitMQConnectionHealthContributor healthContributorRegistry;
    @Mock
    private Message amqpMessage;
    @Mock
    private Publisher.Callback amqpMessageCallback;
    private AutoCloseable openMocks;

    private static <T> Answer<?> delegateAnswer(final T delegate) {
        return invocation -> invocation.getMethod().invoke(delegate, invocation.getArguments());
    }

    private static int messageTypeId(final MessageType messageType) {
        final Integer id = MESSAGE_TYPE_IDS.get(messageType);
        if (id == null) {
            throw new IllegalArgumentException("Unknown message type: " + messageType);
        }
        return id;
    }

    @SuppressWarnings("resource")
    private static void verifyBuildingOfAmqpConnection(final TestSwimRabbitMQPublisherFactory factory, final int numberOfInvocations) {
        verify(factory.environment, times(numberOfInvocations)).connectionBuilder();
        verify(factory.connectionBuilder, times(numberOfInvocations)).uri(MINIMAL_CONFIG.connection().uri());
        verify(factory.connectionBuilder, times(numberOfInvocations)).username(MINIMAL_CONFIG.connection().username());
        verify(factory.connectionBuilder, times(numberOfInvocations)).password(MINIMAL_CONFIG.connection().password());
        final ArgumentCaptor<Resource.StateListener> stateListenersCaptor = ArgumentCaptor.forClass(Resource.StateListener.class);
        verify(factory.connectionBuilder, times(numberOfInvocations)).listeners(stateListenersCaptor.capture());
        assertThat(stateListenersCaptor.getAllValues()).contains(factory.connectionHealthIndicator);
        verify(factory.connectionBuilder, times(numberOfInvocations)).build();
    }

    @SuppressWarnings("resource")
    private static void verifyBuildingOfAmqpPublisher(
            final Connection connection,
            final PublisherBuilder publisherBuilder,
            final int publisherBuilderInvocations) {
        verify(connection, times(1)).publisherBuilder();
        verify(publisherBuilder, times(publisherBuilderInvocations)).build();
    }

    @BeforeEach
    void setUp() {
        openMocks = MockitoAnnotations.openMocks(this);
        when(retryParamsFactory.retryParams(
                any(RetryingPostActionFactories.RetryConfig.class),
                anyString(),
                any(Duration.class),
                anyInt())
        )
                .thenAnswer(invocation ->
                {
                    final ExecutorService executorService = mock(ExecutorService.class, delegateAnswer(MoreExecutors.newDirectExecutorService()));
                    final AbstractRetryingPostAction.RetryParams newRetryParams = new AbstractRetryingPostAction.RetryParams(
                            executorService,
                            invocation.getArgument(2),
                            retryTemplate);
                    capturedRetryParams.add(newRetryParams);
                    return newRetryParams;
                });
    }

    @AfterEach
    void tearDown() throws Exception {
        if (openMocks != null) {
            openMocks.close();
        }
    }

    private TestSwimRabbitMQPublisherFactory newSwimRabbitMQPublisherFactory() {
        return newSwimRabbitMQPublisherFactory(MESSAGE_TYPE_IDS);
    }

    private TestSwimRabbitMQPublisherFactory newSwimRabbitMQPublisherFactory(final BiMap<MessageType, Integer> messageTypeIds) {
        return new TestSwimRabbitMQPublisherFactory(configFactory, retryParamsFactory, healthContributorRegistry, CLOCK, IWXXM_FORMAT_ID, messageTypeIds);
    }

    @Test
    void lazy_amqp_publisher_is_not_instantiated_when_not_invoked() throws Exception {
        final TestSwimRabbitMQPublisherFactory closedFactory;
        try (final TestSwimRabbitMQPublisherFactory factory = newSwimRabbitMQPublisherFactory()) {
            final SwimRabbitMQPublisher publisher = factory.newInstance(MINIMAL_CONFIG);
            verifySwimRabbitMQPublisherInitialization(
                    publisher, factory, MINIMAL_CONFIG, EXPECTED_MESSAGE_CONFIG);
            closedFactory = factory; // automatically closed by try-with-resources
        }

        verify(closedFactory.environment).close();
        verify(capturedRetryParams.getFirst().executor()).shutdown();

        //noinspection resource
        verify(closedFactory.connectionBuilder, never()).build();
        verifyNoInteractions(closedFactory.connection);
        //noinspection resource
        verify(closedFactory.publisherBuilder, never()).build();
        verifyNoInteractions(closedFactory.publisher);
    }

    @Test
    void lazy_amqp_publisher_is_instantiated_upon_first_invocation() throws Exception {
        final TestSwimRabbitMQPublisherFactory closedFactory;
        try (final TestSwimRabbitMQPublisherFactory factory = newSwimRabbitMQPublisherFactory()) {
            final SwimRabbitMQPublisher publisher = factory.newInstance(MINIMAL_CONFIG);

            final Publisher amqpPublisher = verifySwimRabbitMQPublisherInitialization(
                    publisher, factory, MINIMAL_CONFIG, EXPECTED_MESSAGE_CONFIG);
            amqpPublisher.publish(amqpMessage, amqpMessageCallback);

            verifyBuildingOfAmqpConnection(factory, 1);
            verifyBuildingOfAmqpPublisher(factory.connection, factory.publisherBuilder, 1);
            verify(factory.publisher).publish(amqpMessage, amqpMessageCallback);

            closedFactory = factory; // automatically closed by try-with-resources
        }

        verify(closedFactory.environment).close();
        verify(closedFactory.connection).close();
        verify(closedFactory.publisher).close();
        verify(capturedRetryParams.getFirst().executor()).shutdown();
    }

    @Test
    void amqp_connection_is_recreated_when_publisher_closes() throws Exception {
        try (final TestSwimRabbitMQPublisherFactory factory = newSwimRabbitMQPublisherFactory()) {
            final SwimRabbitMQPublisher publisher = factory.newInstance(MINIMAL_CONFIG);

            final Publisher amqpPublisher = verifySwimRabbitMQPublisherInitialization(
                    publisher, factory, MINIMAL_CONFIG, EXPECTED_MESSAGE_CONFIG);
            amqpPublisher.publish(amqpMessage, amqpMessageCallback);

            verifyBuildingOfAmqpConnection(factory, 1);
            verifyBuildingOfAmqpPublisher(factory.connection, factory.publisherBuilder, 1);
            verify(factory.publisher).publish(amqpMessage, amqpMessageCallback);

            // indicate publisher has closed
            final ArgumentCaptor<Resource.StateListener> stateListenersCaptor = ArgumentCaptor.forClass(Resource.StateListener.class);
            verify(factory.publisherBuilder).listeners(stateListenersCaptor.capture());
            final Resource.Context publisherClosedContext = TestAmqpResourceContext.builder()
                    .currentState(Resource.State.CLOSED)
                    .previousState(Resource.State.OPEN)
                    .failureCause(new RuntimeException())
                    .resource(mock(Resource.class))
                    .build();
            stateListenersCaptor.getAllValues()
                    .forEach(listener -> listener.handle(publisherClosedContext));
            verify(factory.publisher).close();
            verify(factory.connection).close();
            // verify no new connection nor publisher has been created yet
            verifyBuildingOfAmqpConnection(factory, 1);
            verifyBuildingOfAmqpPublisher(factory.connection, factory.publisherBuilder, 1);

            amqpPublisher.publish(amqpMessage, amqpMessageCallback);

            verifyBuildingOfAmqpConnection(factory, 2);
            verifyBuildingOfAmqpPublisher(factory.connection2, factory.publisherBuilder, 2);
            verify(factory.publisher2).publish(amqpMessage, amqpMessageCallback);
            verifyNoMoreInteractions(factory.connection, factory.publisher);
        }
    }

    @Test
    void does_not_create_topology_when_not_requested() throws Exception {
        final TestConfig config = MINIMAL_CONFIG.toBuilder()
                .mapTopology(topology -> topology.toBuilder()
                        .mapExchange(exchange -> exchange.toBuilder().create(false).build())
                        .mapQueue(queue -> queue.toBuilder().create(false).build())
                        .build())
                .build();
        try (final TestSwimRabbitMQPublisherFactory factory = newSwimRabbitMQPublisherFactory()) {
            final SwimRabbitMQPublisher publisher = factory.newInstance(config);
            final Publisher amqpPublisher = verifySwimRabbitMQPublisherInitialization(publisher, factory, config, SwimRabbitMQPublisherFactoryTest.EXPECTED_MESSAGE_CONFIG);
            amqpPublisher.publish(amqpMessage, amqpMessageCallback);

            verifyNoInteractions(factory.exchangeSpecification, factory.queueSpecification, factory.bindingSpecification);
        }
    }

    @Test
    void creates_only_queue_and_binding_when_requested() throws Exception {
        final TestConfig config = MINIMAL_CONFIG.toBuilder()
                .mapTopology(topology -> topology.toBuilder()
                        .mapExchange(exchange -> exchange.toBuilder().create(false).build())
                        .mapQueue(queue -> queue.toBuilder().create(true).build())
                        .build())
                .build();
        try (final TestSwimRabbitMQPublisherFactory factory = newSwimRabbitMQPublisherFactory()) {
            final SwimRabbitMQPublisher publisher = factory.newInstance(config);
            final Publisher amqpPublisher = verifySwimRabbitMQPublisherInitialization(publisher, factory, config, SwimRabbitMQPublisherFactoryTest.EXPECTED_MESSAGE_CONFIG);
            amqpPublisher.publish(amqpMessage, amqpMessageCallback);

            final Config.QueueConfig queueConfig = config.topology().queue();
            verify(factory.queueSpecification).name(queueConfig.name());
            verify(factory.queueSpecification).type(Management.QueueType.CLASSIC);
            verify(factory.queueSpecification).autoDelete(false);
            verify(factory.queueSpecification).arguments(Map.of());
            final InOrder queueSpecInOrder = inOrder(factory.queueSpecification);
            queueSpecInOrder.verify(factory.queueSpecification).declare();
            queueSpecInOrder.verifyNoMoreInteractions();

            final Config.ExchangeConfig exchangeConfig = config.topology().exchange();
            verify(factory.bindingSpecification).sourceExchange(exchangeConfig.name());
            verify(factory.bindingSpecification).destinationQueue(queueConfig.name());
            verify(factory.bindingSpecification).key(config.topology().routingKey());
            final InOrder bindingSpecInOrder = inOrder(factory.bindingSpecification);
            bindingSpecInOrder.verify(factory.bindingSpecification).bind();
            bindingSpecInOrder.verifyNoMoreInteractions();

            verifyNoInteractions(factory.exchangeSpecification);
        }
    }

    @Test
    void creates_only_exchange_when_requested() throws Exception {
        final TestConfig config = MINIMAL_CONFIG.toBuilder()
                .mapTopology(topology -> topology.toBuilder()
                        .mapExchange(exchange -> exchange.toBuilder().create(true).build())
                        .mapQueue(queue -> queue.toBuilder().create(false).build())
                        .build())
                .build();
        try (final TestSwimRabbitMQPublisherFactory factory = newSwimRabbitMQPublisherFactory()) {
            final SwimRabbitMQPublisher publisher = factory.newInstance(config);
            final Publisher amqpPublisher = verifySwimRabbitMQPublisherInitialization(publisher, factory, config, SwimRabbitMQPublisherFactoryTest.EXPECTED_MESSAGE_CONFIG);
            amqpPublisher.publish(amqpMessage, amqpMessageCallback);

            final Config.ExchangeConfig exchangeConfig = config.topology().exchange();
            verify(factory.exchangeSpecification).name(exchangeConfig.name());
            verify(factory.exchangeSpecification).type(Management.ExchangeType.DIRECT);
            verify(factory.exchangeSpecification).autoDelete(false);
            verify(factory.exchangeSpecification).arguments(Map.of());
            final InOrder exchangeSpecInOrder = inOrder(factory.exchangeSpecification);
            exchangeSpecInOrder.verify(factory.exchangeSpecification).declare();
            exchangeSpecInOrder.verifyNoMoreInteractions();

            verifyNoInteractions(factory.queueSpecification, factory.bindingSpecification);
        }
    }

    @Test
    void creates_topology_when_requested() throws Exception {
        final TestConfig config = MINIMAL_CONFIG.toBuilder()
                .id("test-id-topology")
                .topology(TestTopologyConfig.builder()
                        .routingKey("aviation-message-archiver-test-routing1")
                        .exchange(TestExchangeConfig.builder()
                                .name("aviation-message-archiver-test-exchange1")
                                .type(Management.ExchangeType.TOPIC)
                                .autoDelete(true)
                                .arguments(Map.of(
                                        "e-testarg-1", "e-testarg-1-value",
                                        "e-testarg-2", "e-testarg-2-value"))
                                .create(true)
                                .build())
                        .queue(TestQueueConfig.builder()
                                .name("aviation-message-archiver-test-queue1")
                                .type(Management.QueueType.QUORUM)
                                .autoDelete(true)
                                .arguments(Map.of(
                                        "q-testarg-1", "q-testarg-1-value",
                                        "q-testarg-2", "q-testarg-2-value"
                                ))
                                .create(true)
                                .build())
                        .build())
                .build();
        try (final TestSwimRabbitMQPublisherFactory factory = newSwimRabbitMQPublisherFactory()) {
            final SwimRabbitMQPublisher publisher = factory.newInstance(config);
            final Publisher amqpPublisher = verifySwimRabbitMQPublisherInitialization(
                    publisher, factory, config, EXPECTED_MESSAGE_CONFIG);
            amqpPublisher.publish(amqpMessage, amqpMessageCallback);

            final Config.ExchangeConfig exchangeConfig = config.topology().exchange();
            verify(factory.exchangeSpecification).name(exchangeConfig.name());
            verify(factory.exchangeSpecification).type(exchangeConfig.type().orElseThrow());
            verify(factory.exchangeSpecification).autoDelete(exchangeConfig.autoDelete().orElseThrow());
            verify(factory.exchangeSpecification).arguments(exchangeConfig.arguments().orElseThrow());
            final InOrder exchangeSpecInOrder = inOrder(factory.exchangeSpecification);
            exchangeSpecInOrder.verify(factory.exchangeSpecification).declare();
            exchangeSpecInOrder.verifyNoMoreInteractions();

            final Config.QueueConfig queueConfig = config.topology().queue();
            verify(factory.queueSpecification).name(queueConfig.name());
            verify(factory.queueSpecification).type(queueConfig.type().orElseThrow());
            verify(factory.queueSpecification).autoDelete(queueConfig.autoDelete().orElseThrow());
            verify(factory.queueSpecification).arguments(queueConfig.arguments().orElseThrow());
            final InOrder queueSpecInOrder = inOrder(factory.queueSpecification);
            queueSpecInOrder.verify(factory.queueSpecification).declare();
            queueSpecInOrder.verifyNoMoreInteractions();

            verify(factory.bindingSpecification).sourceExchange(exchangeConfig.name());
            verify(factory.bindingSpecification).destinationQueue(queueConfig.name());
            verify(factory.bindingSpecification).key(config.topology().routingKey());
            final InOrder bindingSpecInOrder = inOrder(factory.bindingSpecification);
            bindingSpecInOrder.verify(factory.bindingSpecification).bind();
            bindingSpecInOrder.verifyNoMoreInteractions();
        }
    }

    @Test
    void creates_topology_with_defaults() throws Exception {
        final TestConfig config = MINIMAL_CONFIG.toBuilder()
                .mapTopology(topology -> topology.toBuilder()
                        .mapExchange(exchange -> exchange.toBuilder().create(true).build())
                        .mapQueue(queue -> queue.toBuilder().create(true).build())
                        .build())
                .build();
        try (final TestSwimRabbitMQPublisherFactory factory = newSwimRabbitMQPublisherFactory()) {
            final SwimRabbitMQPublisher publisher = factory.newInstance(config);
            final Publisher amqpPublisher = verifySwimRabbitMQPublisherInitialization(publisher, factory, config, SwimRabbitMQPublisherFactoryTest.EXPECTED_MESSAGE_CONFIG);
            amqpPublisher.publish(amqpMessage, amqpMessageCallback);

            final Config.ExchangeConfig exchangeConfig = config.topology().exchange();
            verify(factory.exchangeSpecification).name(exchangeConfig.name());
            verify(factory.exchangeSpecification).type(Management.ExchangeType.DIRECT);
            verify(factory.exchangeSpecification).autoDelete(false);
            verify(factory.exchangeSpecification).arguments(Map.of());
            final InOrder exchangeSpecInOrder = inOrder(factory.exchangeSpecification);
            exchangeSpecInOrder.verify(factory.exchangeSpecification).declare();
            exchangeSpecInOrder.verifyNoMoreInteractions();

            final Config.QueueConfig queueConfig = config.topology().queue();
            verify(factory.queueSpecification).name(queueConfig.name());
            verify(factory.queueSpecification).type(Management.QueueType.CLASSIC);
            verify(factory.queueSpecification).autoDelete(false);
            verify(factory.queueSpecification).arguments(Map.of());
            final InOrder queueSpecInOrder = inOrder(factory.queueSpecification);
            queueSpecInOrder.verify(factory.queueSpecification).declare();
            queueSpecInOrder.verifyNoMoreInteractions();

            verify(factory.bindingSpecification).sourceExchange(exchangeConfig.name());
            verify(factory.bindingSpecification).destinationQueue(queueConfig.name());
            verify(factory.bindingSpecification).key(config.topology().routingKey());
            final InOrder bindingSpecInOrder = inOrder(factory.bindingSpecification);
            bindingSpecInOrder.verify(factory.bindingSpecification).bind();
            bindingSpecInOrder.verifyNoMoreInteractions();
        }
    }

    @Test
    void ignores_missing_message_types_correctly() throws Exception {
        final BiMap<MessageType, Integer> messageTypeIds = ImmutableBiMap.<MessageType, Integer>builder()
                .put(MessageType.SIGMET, 5)
                .put(MessageType.SPACE_WEATHER_ADVISORY, 6)
                .build();
        try (final TestSwimRabbitMQPublisherFactory factory = newSwimRabbitMQPublisherFactory(messageTypeIds)) {
            final SwimRabbitMQPublisher publisher = factory.newInstance(MINIMAL_CONFIG);
            final SwimRabbitMQPublisher.MessageConfig expectedMessageConfig = EXPECTED_MESSAGE_CONFIG.toBuilder()
                    .clearPriorities()
                    .addPriorities(
                            SwimRabbitMQPublisher.MessageConfig.PriorityDescriptor.builder()
                                    .setMessageType(messageTypeId(MessageType.SIGMET))
                                    .setPriority(7)
                    )
                    .build();
            verifySwimRabbitMQPublisherInitialization(publisher, factory, SwimRabbitMQPublisherFactoryTest.MINIMAL_CONFIG, expectedMessageConfig);
        }
    }

    @SuppressWarnings("resource")
    private Publisher verifySwimRabbitMQPublisherInitialization(
            final SwimRabbitMQPublisher publisher,
            final TestSwimRabbitMQPublisherFactory factory,
            final Config config,
            final SwimRabbitMQPublisher.MessageConfig messageConfig) {
        assertThat(publisher).isNotNull();
        assertThat(factory.createdInstances).hasSize(1);
        assertThat(publisher).isSameAs(factory.createdInstances.getFirst());
        assertThat(publisher.toString()).isEqualTo("SwimRabbitMQPublisher(%s)".formatted(config.id()));

        verify(factory.delegateMock).newConnectionHealthIndicator(CLOCK);
        verify(factory.delegateMock).newPublisherHealthIndicator(CLOCK);
        verify(factory.amqpEnvironmentBuilder).build();
        verify(healthContributorRegistry).registerIndicators(config.id(), factory.connectionHealthIndicator, factory.publisherHealthIndicator);
        assertThat(capturedRetryParams).hasSize(1);
        final ArgumentCaptor<Publisher> amqpPublisherCaptor = ArgumentCaptor.forClass(Publisher.class);
        verify(factory.delegateMock).newSwimRabbitMQPublisher(
                eq(capturedRetryParams.getFirst()),
                eq(config.id()),
                amqpPublisherCaptor.capture(),
                eq(factory.publisherHealthIndicator),
                eq(messageConfig)
        );
        verifyNoInteractions(
                factory.environment,
                factory.connectionBuilder,
                factory.connection,
                factory.publisherBuilder,
                factory.publisher);
        return amqpPublisherCaptor.getValue();
    }

    @FreeBuilder
    static abstract class TestConfig implements Config {
        public static Builder builder() {
            return new Builder();
        }

        @Override
        public abstract TestConnectionConfig connection();

        @Override
        public abstract TestTopologyConfig topology();

        @Override
        public abstract TestRetryConfig retry();

        public abstract Builder toBuilder();

        static class Builder extends SwimRabbitMQPublisherFactoryTest_TestConfig_Builder {
            Builder() {
            }
        }
    }

    @FreeBuilder
    static abstract class TestConnectionConfig implements Config.ConnectionConfig {
        public static Builder builder() {
            return new Builder();
        }

        static class Builder extends SwimRabbitMQPublisherFactoryTest_TestConnectionConfig_Builder {
            Builder() {
            }
        }
    }

    @FreeBuilder
    static abstract class TestTopologyConfig implements Config.TopologyConfig {
        public static Builder builder() {
            return new Builder();
        }

        @Override
        public abstract TestExchangeConfig exchange();

        @Override
        public abstract TestQueueConfig queue();

        public abstract Builder toBuilder();

        static class Builder extends SwimRabbitMQPublisherFactoryTest_TestTopologyConfig_Builder {
            Builder() {
            }
        }
    }

    @FreeBuilder
    static abstract class TestExchangeConfig implements Config.ExchangeConfig {
        public static Builder builder() {
            return new Builder();
        }

        public abstract Builder toBuilder();

        static class Builder extends SwimRabbitMQPublisherFactoryTest_TestExchangeConfig_Builder {
            Builder() {
            }
        }
    }

    @FreeBuilder
    static abstract class TestQueueConfig implements Config.QueueConfig {
        public static Builder builder() {
            return new Builder();
        }

        public abstract Builder toBuilder();

        static class Builder extends SwimRabbitMQPublisherFactoryTest_TestQueueConfig_Builder {
            Builder() {
            }
        }
    }

    @FreeBuilder
    static abstract class TestMessageConfig implements Config.MessageConfig {
        public static Builder builder() {
            return new Builder();
        }

        static class Builder extends SwimRabbitMQPublisherFactoryTest_TestMessageConfig_Builder {
            Builder() {
            }
        }
    }

    @FreeBuilder
    static abstract class TestPriorityDescriptor implements Config.PriorityDescriptor {
        public static Builder builder() {
            return new Builder();
        }

        static class Builder extends SwimRabbitMQPublisherFactoryTest_TestPriorityDescriptor_Builder {
            Builder() {
            }
        }
    }

    @FreeBuilder
    static abstract class TestRetryConfig implements RetryingPostActionFactories.RetryConfig {
        public static Builder builder() {
            return new Builder();
        }

        static class Builder extends SwimRabbitMQPublisherFactoryTest_TestRetryConfig_Builder {
            Builder() {
            }
        }
    }

    @FreeBuilder
    static abstract class TestAmqpResourceContext implements Resource.Context {
        public static Builder builder() {
            return new Builder();
        }

        static class Builder extends SwimRabbitMQPublisherFactoryTest_TestAmqpResourceContext_Builder {
            Builder() {
            }
        }
    }

    static class TestSwimRabbitMQPublisherFactory extends SwimRabbitMQPublisherFactory implements AutoCloseable {
        final List<SwimRabbitMQPublisher> createdInstances = new ArrayList<>();

        private final AutoCloseable openMocks;

        @Mock(answer = Answers.RETURNS_SELF)
        AmqpEnvironmentBuilder amqpEnvironmentBuilder;
        @Mock
        Environment environment;
        @Mock
        Connection connection;
        @Mock
        Connection connection2;
        @Mock
        Management management;
        @Mock(answer = Answers.RETURNS_SELF)
        Management.ExchangeSpecification exchangeSpecification;
        @Mock(answer = Answers.RETURNS_SELF)
        Management.QueueSpecification queueSpecification;
        @Mock(answer = Answers.RETURNS_SELF)
        Management.BindingSpecification bindingSpecification;
        @Mock(answer = Answers.RETURNS_SELF)
        PublisherBuilder publisherBuilder;
        @Mock
        Publisher publisher;
        @Mock
        Publisher publisher2;
        @Mock(answer = Answers.RETURNS_SELF)
        ConnectionBuilder connectionBuilder;
        @Mock
        RabbitMQConnectionHealthIndicator connectionHealthIndicator;
        @Mock
        RabbitMQPublisherHealthIndicator publisherHealthIndicator;
        @Mock
        SwimRabbitMQPublisherFactory delegateMock;

        public TestSwimRabbitMQPublisherFactory(
                final ObjectFactoryConfigFactory configFactory,
                final RetryingPostActionFactories.RetryParamsFactory retryParamsFactory,
                final SwimRabbitMQConnectionHealthContributor healthContributorRegistry,
                final Clock clock,
                final int iwxxmFormatId,
                final BiMap<MessageType, Integer> messageTypeIds) {
            super(configFactory, retryParamsFactory, healthContributorRegistry, clock, iwxxmFormatId, messageTypeIds);
            this.openMocks = MockitoAnnotations.openMocks(this);
            when(amqpEnvironmentBuilder.build()).thenReturn(environment);
            when(environment.connectionBuilder()).thenReturn(connectionBuilder);
            when(connectionBuilder.build()).thenReturn(connection, connection2);
            Arrays.asList(connection, connection2).forEach(connectionMock -> {
                when(connectionMock.publisherBuilder()).thenReturn(publisherBuilder);
                when(connectionMock.management()).thenReturn(management);
            });
            when(management.exchange()).thenReturn(exchangeSpecification);
            when(management.queue()).thenReturn(queueSpecification);
            when(management.binding()).thenReturn(bindingSpecification);
            when(publisherBuilder.build()).thenReturn(publisher, publisher2);
            when(delegateMock.newAmqpEnvironmentBuilder()).thenReturn(amqpEnvironmentBuilder);
            when(delegateMock.newConnectionHealthIndicator(any())).thenReturn(connectionHealthIndicator);
            when(delegateMock.newPublisherHealthIndicator(any())).thenReturn(publisherHealthIndicator);
        }

        @Override
        AmqpEnvironmentBuilder newAmqpEnvironmentBuilder() {
            return delegateMock.newAmqpEnvironmentBuilder();
        }

        @Override
        RabbitMQConnectionHealthIndicator newConnectionHealthIndicator(final Clock clock) {
            return delegateMock.newConnectionHealthIndicator(clock);
        }

        @Override
        RabbitMQPublisherHealthIndicator newPublisherHealthIndicator(final Clock clock) {
            return delegateMock.newPublisherHealthIndicator(clock);
        }

        @Override
        SwimRabbitMQPublisher newSwimRabbitMQPublisher(
                final AbstractRetryingPostAction.RetryParams retryParams,
                final String instanceId,
                final Publisher publisher,
                final Consumer<Publisher.Context> publisherHealthIndicator,
                final SwimRabbitMQPublisher.MessageConfig messageConfig) {
            delegateMock.newSwimRabbitMQPublisher(
                    retryParams, instanceId, publisher, publisherHealthIndicator, messageConfig);
            // Return real instance instead of a mock, because AbstractRetryingPostAction cannot be mocked properly
            // due to final close() method. Invocation to mock close() attempts to shutdown a null executor.
            final SwimRabbitMQPublisher realInstance = super.newSwimRabbitMQPublisher(
                    retryParams, instanceId, publisher, publisherHealthIndicator, messageConfig);
            createdInstances.add(realInstance);
            return realInstance;
        }

        @Override
        public void close() throws Exception {
            super.close();
            openMocks.close();
        }
    }
}
