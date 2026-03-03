package fi.fmi.avi.archiver.config.factory.postaction;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.rabbitmq.client.amqp.*;
import com.rabbitmq.client.amqp.impl.AmqpEnvironmentBuilder;
import fi.fmi.avi.archiver.config.model.PostActionFactory;
import fi.fmi.avi.archiver.message.processor.postaction.AbstractRetryingPostAction;
import fi.fmi.avi.archiver.message.processor.postaction.SwimRabbitMQPublisher;
import fi.fmi.avi.archiver.spring.healthcontributor.RabbitMQConnectionHealthIndicator;
import fi.fmi.avi.archiver.spring.healthcontributor.RabbitMQPublisherHealthIndicator;
import fi.fmi.avi.archiver.spring.healthcontributor.SwimRabbitMQConnectionHealthContributor;
import fi.fmi.avi.archiver.util.instantiation.AbstractTypedConfigObjectFactory;
import fi.fmi.avi.archiver.util.instantiation.ObjectFactoryConfig;
import fi.fmi.avi.archiver.util.instantiation.ObjectFactoryConfigFactory;
import fi.fmi.avi.model.AviationWeatherMessage;
import fi.fmi.avi.model.MessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.time.Clock;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static fi.fmi.avi.archiver.logging.GenericStructuredLoggable.loggableValue;
import static java.util.Objects.requireNonNull;

public class SwimRabbitMQPublisherFactory
        extends AbstractTypedConfigObjectFactory<SwimRabbitMQPublisher, SwimRabbitMQPublisherFactory.Config>
        implements PostActionFactory<SwimRabbitMQPublisher>, AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(SwimRabbitMQPublisherFactory.class);

    private static final List<SwimRabbitMQPublisher.StaticApplicationProperties> APPLICATION_PROPERTY_DESCRIPTORS =
            ImmutableList.of(
                    new SwimRabbitMQPublisher.StaticApplicationProperties(
                            MessageType.METAR,
                            "weather.aviation.metar",
                            "https://eur-registry.swim.aero/services/eurocontrol-iwxxm-metar-speci-subscription-and-request-service-10",
                            "AD"),
                    new SwimRabbitMQPublisher.StaticApplicationProperties(
                            MessageType.SPECI,
                            "weather.aviation.metar",
                            "https://eur-registry.swim.aero/services/eurocontrol-iwxxm-metar-speci-subscription-and-request-service-10",
                            "AD"),
                    new SwimRabbitMQPublisher.StaticApplicationProperties(
                            MessageType.TAF,
                            "weather.aviation.taf",
                            "https://eur-registry.swim.aero/services/eurocontrol-iwxxm-taf-subscription-and-request-service-10",
                            "AD"),
                    new SwimRabbitMQPublisher.StaticApplicationProperties(
                            MessageType.SIGMET,
                            "weather.aviation.sigmet",
                            "https://eur-registry.swim.aero/services/eurocontrol-iwxxm-sigmet-subscription-and-request-service-10",
                            "FIR")
            );

    private static final List<Config.PriorityDescriptor> DEFAULT_PRIORITIES = List.of(
            new ImmutablePriorityDescriptor(MessageType.SIGMET, 7),
            new ImmutablePriorityDescriptor(MessageType.SPECI, 6),
            new ImmutablePriorityDescriptor(MessageType.TAF, AviationWeatherMessage.ReportStatus.AMENDMENT, 6),
            new ImmutablePriorityDescriptor(MessageType.TAF, 5),
            new ImmutablePriorityDescriptor(MessageType.METAR, 4)
    );

    private final RetryingPostActionFactories.RetryParamsFactory retryParamsFactory;
    private final SwimRabbitMQConnectionHealthContributor healthContributorRegistry;
    private final Clock clock;
    private final List<AutoCloseable> closeableResources = new ArrayList<>();
    private final int iwxxmFormatId;
    private final BiMap<MessageType, Integer> messageTypeIds;
    private final Map<Integer, SwimRabbitMQPublisher.StaticApplicationProperties> staticAppPropsByTypeId;

    public SwimRabbitMQPublisherFactory(
            final ObjectFactoryConfigFactory configFactory,
            final RetryingPostActionFactories.RetryParamsFactory retryParamsFactory,
            final SwimRabbitMQConnectionHealthContributor healthContributorRegistry,
            final Clock clock,
            final int iwxxmFormatId,
            final BiMap<MessageType, Integer> messageTypeIds) {
        super(configFactory);
        this.retryParamsFactory = requireNonNull(retryParamsFactory, "retryParamsFactory");
        this.healthContributorRegistry = requireNonNull(healthContributorRegistry, "healthContributorRegistry");
        this.clock = requireNonNull(clock, "clock");
        this.iwxxmFormatId = iwxxmFormatId;
        this.messageTypeIds = requireNonNull(messageTypeIds, "messageTypeIds");

        this.staticAppPropsByTypeId = APPLICATION_PROPERTY_DESCRIPTORS.stream()
                .filter(applicationProperties -> messageTypeIds.containsKey(applicationProperties.type()))
                .collect(ImmutableMap.toImmutableMap(
                        applicationProperties -> requireNonNull(messageTypeIds.get(requireNonNull(applicationProperties).type())),
                        Function.identity()
                ));
    }

    private static <T extends AutoCloseable> T createLazyForwardingProxy(
            final Class<T> type,
            final Supplier<T> factory,
            final AtomicReference<T> instanceRef) {
        return type.cast(Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class<?>[]{type},
                (proxy, method, args) -> {
                    T instance = instanceRef.get();
                    while (instance == null) {
                        final T newInstance = factory.get();
                        if (instanceRef.compareAndSet(null, newInstance)) {
                            instance = newInstance;
                        } else {
                            try {
                                newInstance.close();
                            } catch (final Exception e) {
                                LOGGER.warn("Failed to close unused instance", e);
                            }
                            instance = instanceRef.get();
                        }
                    }
                    try {
                        return method.invoke(instance, args);
                    } catch (final InvocationTargetException ite) {
                        throw ite.getTargetException();
                    }
                }
        ));
    }

    private static void log(final Resource.Context context) {
        switch (context.currentState()) {
            case OPENING -> LOGGER.info("RabbitMQ connecting...");
            case OPEN -> LOGGER.info("RabbitMQ connection established");
            case RECOVERING -> LOGGER.info("RabbitMQ connection recovering...");
            case CLOSING -> LOGGER.info("RabbitMQ connection closing...");
            case CLOSED -> {
                if (context.failureCause() != null) {
                    LOGGER.error("RabbitMQ connection lost", context.failureCause());
                } else {
                    LOGGER.info("RabbitMQ connection closed");
                }
            }
            default -> LOGGER.error("RabbitMQ connection in unknown state");
        }
    }

    private static void createTopology(final Connection connection, final Config.TopologyConfig config) {
        try (final Management management = connection.management()) {
            final TopologyElements elements = config.create();
            final Config.ExchangeConfig exchangeConfig = config.exchange();
            final Config.QueueConfig queueConfig = config.queue();

            if (elements == TopologyElements.ALL || elements == TopologyElements.EXCHANGE) {
                management.exchange()
                        .name(exchangeConfig.name())
                        .type(exchangeConfig.type().orElse(Management.ExchangeType.DIRECT))
                        .autoDelete(exchangeConfig.autoDelete().orElse(false))
                        .arguments(exchangeConfig.arguments().orElse(Collections.emptyMap()))
                        .declare();
            }
            if (elements == TopologyElements.ALL || elements == TopologyElements.QUEUE_AND_BINDING) {
                management.queue()
                        .name(queueConfig.name())
                        .type(queueConfig.type().orElse(Management.QueueType.CLASSIC))
                        .exclusive(queueConfig.exclusive().orElse(false))
                        .autoDelete(queueConfig.autoDelete().orElse(false))
                        .arguments(queueConfig.arguments().orElse(Collections.emptyMap()))
                        .declare();
            }
            if (elements == TopologyElements.ALL || elements == TopologyElements.QUEUE_AND_BINDING
                    || elements == TopologyElements.BINDING) {
                management.binding()
                        .sourceExchange(exchangeConfig.name())
                        .destinationQueue(queueConfig.name())
                        .key(config.routingKey())
                        .bind();
            }
        } catch (final Exception e) {
            LOGGER.error("Failed to create AMQP topology for exchange '{}', queue '{}', routing key '{}'",
                    config.exchange(), config.queue(), config.routingKey(), e);
            throw e;
        }
    }

    @Override
    public Class<SwimRabbitMQPublisher> getType() {
        return SwimRabbitMQPublisher.class;
    }

    @Override
    public Class<Config> getConfigType() {
        return Config.class;
    }

    @Override
    public SwimRabbitMQPublisher newInstance(final Config config) {
        requireNonNull(config, "config");
        final Config.ConnectionConfig connectionConfig = config.connection();
        final RabbitMQConnectionHealthIndicator connectionHealthIndicator = newConnectionHealthIndicator(clock);
        final RabbitMQPublisherHealthIndicator publisherHealthIndicator = newPublisherHealthIndicator(clock);
        final Environment environment = registerCloseable(newAmqpEnvironmentBuilder().build());

        final AtomicReference<Connection> connectionRef = new AtomicReference<>();
        final AtomicReference<Publisher> publisherRef = new AtomicReference<>();

        final Connection connection = createLazyForwardingProxy(Connection.class, () -> registerCloseable(environment
                .connectionBuilder()
                .username(connectionConfig.username())
                .password(connectionConfig.password())
                .uri(connectionConfig.uri())
                .listeners(SwimRabbitMQPublisherFactory::log, connectionHealthIndicator)
                .build()), connectionRef);

        final Publisher publisher = createLazyForwardingProxy(Publisher.class, () -> {
            final Config.TopologyConfig topologyConfig = config.topology();
            if (topologyConfig.create() != TopologyElements.NONE) {
                createTopology(connection, topologyConfig);
            }

            return registerCloseable(connection.publisherBuilder()
                    .listeners(context -> {
                        if (context.currentState() == Resource.State.CLOSED && context.failureCause() != null) {
                            LOGGER.error("AMQP publisher closed unexpectedly - connection and publisher will be recreated on next publish attempt");
                            unregisterAndClose(connectionRef, "connection");
                            unregisterAndClose(publisherRef, "publisher");
                        }
                    })
                    .build());
        }, publisherRef);

        final SwimRabbitMQPublisher action = registerCloseable(newSwimRabbitMQPublisher(
                retryParamsFactory.retryParams(config.retry(), getInstanceName(config.id()),
                        config.publishTimeout().orElse(Duration.ofSeconds(30)), config.publisherQueueCapacity()),
                config.id(), publisher, publisherHealthIndicator,
                toPublisherMessageConfig(config.id(), config.topology().exchange().name(),
                        config.message().orElse(null))));
        healthContributorRegistry.registerIndicators(config.id(), connectionHealthIndicator, publisherHealthIndicator);
        return action;
    }

    private String getInstanceName(final String instanceId) {
        return getName() + '(' + instanceId + ')';
    }

    private SwimRabbitMQPublisher.MessageConfig toPublisherMessageConfig(
            final String configId, final String exchangeName, @Nullable final Config.MessageConfig factoryConfig) {
        final SwimRabbitMQPublisher.MessageConfig.Builder builder = SwimRabbitMQPublisher.MessageConfig.builder()
                .setExchange(exchangeName);
        if (factoryConfig == null) {
            builder.addAllPriorities(getDefaultPriorities()
                    .map(this::toPublisherPriorityDescriptor));
        } else {
            factoryConfig.encoding().ifPresent(builder::setEncoding);
            factoryConfig.expiryTime().ifPresent(builder::setExpiryTime);
            builder.addAllPriorities(factoryConfig.priorities()
                    .map(List::stream)
                    .orElseGet(this::getDefaultPriorities)
                    .map(this::toPublisherPriorityDescriptor));
        }
        final SwimRabbitMQPublisher.MessageConfig publisherConfig = builder.build();
        if (publisherConfig.getPriorities().isEmpty()) {
            LOGGER.warn("Publisher <{}> priorities is empty. Using default priority <{}> for all messages.",
                    loggableValue("postActionId", configId), SwimRabbitMQPublisher.MessageConfig.DEFAULT_PRIORITY);
        }
        return publisherConfig;
    }

    private Stream<Config.PriorityDescriptor> getDefaultPriorities() {
        return DEFAULT_PRIORITIES.stream()
                // Omit priority descriptors for message types not declared in config
                .filter(priorityDescriptor -> priorityDescriptor.type()
                        .map(messageTypeIds::containsKey)
                        .orElse(true));
    }

    private SwimRabbitMQPublisher.MessageConfig.PriorityDescriptor toPublisherPriorityDescriptor(final Config.PriorityDescriptor factoryDescriptor) {
        return SwimRabbitMQPublisher.MessageConfig.PriorityDescriptor.builder()
                .setMessageType(factoryDescriptor.type()
                        .map(messageTypeIds::get)
                        .map(OptionalInt::of)
                        .orElseGet(OptionalInt::empty))
                .setReportStatus(factoryDescriptor.status())
                .setPriority(factoryDescriptor.priority())
                .build();
    }

    @VisibleForTesting
    AmqpEnvironmentBuilder newAmqpEnvironmentBuilder() {
        return new AmqpEnvironmentBuilder();
    }

    @VisibleForTesting
    RabbitMQConnectionHealthIndicator newConnectionHealthIndicator(final Clock clock) {
        return new RabbitMQConnectionHealthIndicator(clock);
    }

    @VisibleForTesting
    RabbitMQPublisherHealthIndicator newPublisherHealthIndicator(final Clock clock) {
        return new RabbitMQPublisherHealthIndicator(clock);
    }

    @VisibleForTesting
    SwimRabbitMQPublisher newSwimRabbitMQPublisher(
            final AbstractRetryingPostAction.RetryParams retryParams, final String instanceId, final Publisher publisher,
            final Consumer<Publisher.Context> publisherHealthIndicator, final SwimRabbitMQPublisher.MessageConfig messageConfig) {
        return new SwimRabbitMQPublisher(retryParams, instanceId, publisher, publisherHealthIndicator, clock, iwxxmFormatId,
                staticAppPropsByTypeId, messageConfig);
    }

    private <T extends AutoCloseable> T registerCloseable(final T closeableResource) {
        synchronized (closeableResources) {
            closeableResources.add(closeableResource);
        }
        return closeableResource;
    }

    private <T extends AutoCloseable> T unregisterCloseable(final T closeableResource) {
        synchronized (closeableResources) {
            closeableResources.remove(closeableResource);
        }
        return closeableResource;
    }

    private void unregisterAndClose(final AtomicReference<? extends AutoCloseable> reference, final String resourceType) {
        final AutoCloseable resource = reference.getAndSet(null);
        if (resource != null) {
            try {
                unregisterCloseable(resource).close();
            } catch (final Exception e) {
                LOGGER.warn("Failed to close {} during cleanup", resourceType, e);
            }
        }
    }

    @Override
    public void close() throws Exception {
        synchronized (closeableResources) {
            closeableResources.forEach(closeableResource -> {
                try {
                    closeableResource.close();
                } catch (final Exception exception) {
                    LOGGER.error("Error closing resource <{}>", closeableResource, exception);
                }
            });
            closeableResources.clear();
        }
    }

    public enum TopologyElements {ALL, EXCHANGE, QUEUE_AND_BINDING, BINDING, NONE}

    public interface Config extends ObjectFactoryConfig {
        String id();

        int publisherQueueCapacity();

        Optional<Duration> publishTimeout();

        ConnectionConfig connection();

        TopologyConfig topology();

        RetryingPostActionFactories.RetryConfig retry();

        Optional<MessageConfig> message();

        interface ConnectionConfig extends ObjectFactoryConfig {
            String uri();

            String username();

            String password();
        }

        interface TopologyConfig extends ObjectFactoryConfig {
            String routingKey();

            ExchangeConfig exchange();

            QueueConfig queue();

            TopologyElements create();
        }

        interface ExchangeConfig extends ObjectFactoryConfig {
            String name();

            Optional<Management.ExchangeType> type();

            Optional<Boolean> autoDelete();

            Optional<Map<String, Object>> arguments();
        }

        interface QueueConfig extends ObjectFactoryConfig {
            String name();

            Optional<Management.QueueType> type();

            Optional<Boolean> exclusive();

            Optional<Boolean> autoDelete();

            Optional<Map<String, Object>> arguments();
        }

        /**
         * Message configuration.
         *
         * <p>
         * Currently, all properties are {@code Optional}. In case any mandatory properties are introduced,
         * {@link Config#message()} must also be made mandatory, and this note may be removed.
         * </p>
         */
        interface MessageConfig extends ObjectFactoryConfig {
            Optional<List<PriorityDescriptor>> priorities();

            Optional<SwimRabbitMQPublisher.ContentEncoding> encoding();

            Optional<Duration> expiryTime();
        }

        interface PriorityDescriptor extends ObjectFactoryConfig {
            Optional<MessageType> type();

            Optional<AviationWeatherMessage.ReportStatus> status();

            int priority();
        }
    }

    private record ImmutablePriorityDescriptor(
            MessageType nullableType,
            AviationWeatherMessage.ReportStatus nullableStatus,
            int priority)
            implements Config.PriorityDescriptor {

        ImmutablePriorityDescriptor(final MessageType type, final int priority) {
            this(type, null, priority);
        }

        @Override
        public Optional<MessageType> type() {
            return Optional.ofNullable(nullableType);
        }

        @Override
        public Optional<AviationWeatherMessage.ReportStatus> status() {
            return Optional.ofNullable(nullableStatus);
        }
    }
}