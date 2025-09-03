package fi.fmi.avi.archiver.config.factory.postaction;

import com.google.common.annotations.VisibleForTesting;
import com.rabbitmq.client.amqp.*;
import com.rabbitmq.client.amqp.impl.AmqpEnvironmentBuilder;
import fi.fmi.avi.archiver.config.model.PostActionFactory;
import fi.fmi.avi.archiver.message.processor.postaction.SwimRabbitMQPublisher;
import fi.fmi.avi.archiver.spring.healthcontributor.RabbitMQConnectionHealthIndicator;
import fi.fmi.avi.archiver.spring.healthcontributor.RabbitMQPublisherHealthIndicator;
import fi.fmi.avi.archiver.spring.healthcontributor.SwimRabbitMQConnectionHealthContributor;
import fi.fmi.avi.archiver.util.instantiation.AbstractTypedConfigObjectFactory;
import fi.fmi.avi.archiver.util.instantiation.ObjectFactoryConfig;
import fi.fmi.avi.archiver.util.instantiation.ObjectFactoryConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.listener.RetryListenerSupport;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.retry.support.RetryTemplateBuilder;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.time.Clock;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static fi.fmi.avi.archiver.logging.GenericStructuredLoggable.loggableValue;
import static fi.fmi.avi.archiver.spring.retry.ArchiverRetryContexts.LOGGING_CONTEXT;
import static fi.fmi.avi.archiver.spring.retry.ArchiverRetryContexts.RETRY_COUNT_LOGNAME;
import static java.util.Objects.requireNonNull;

public class SwimRabbitMQPublisherFactory
        extends AbstractTypedConfigObjectFactory<SwimRabbitMQPublisher, SwimRabbitMQPublisherFactory.Config>
        implements PostActionFactory<SwimRabbitMQPublisher>, AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(SwimRabbitMQPublisherFactory.class);

    private final SwimRabbitMQConnectionHealthContributor healthContributorRegistry;
    private final Clock clock;
    private final List<AutoCloseable> closeableResources = new ArrayList<>();

    public SwimRabbitMQPublisherFactory(
            final ObjectFactoryConfigFactory configFactory,
            final SwimRabbitMQConnectionHealthContributor healthContributorRegistry,
            final Clock clock) {
        super(configFactory);
        this.healthContributorRegistry = requireNonNull(healthContributorRegistry, "healthContributorRegistry");
        this.clock = requireNonNull(clock, "clock");
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
                        T newInstance = factory.get();
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
                        final Throwable cause = ite.getTargetException();
                        if (cause instanceof RuntimeException re) {
                            throw re;
                        }
                        if (cause instanceof Error err) {
                            throw err;
                        }
                        throw new RuntimeException(cause);
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
            final Config.ExchangeConfig exchangeConfig = config.getExchange();
            final Config.QueueConfig queueConfig = config.getQueue();
            management.exchange()
                    .name(exchangeConfig.getName())
                    .type(exchangeConfig.getType().orElse(Management.ExchangeType.DIRECT))
                    .autoDelete(exchangeConfig.autoDelete().orElse(false))
                    .arguments(exchangeConfig.getArguments().orElse(Collections.emptyMap()))
                    .declare();
            management.queue()
                    .name(queueConfig.getName())
                    .type(queueConfig.getType().orElse(Management.QueueType.CLASSIC))
                    .autoDelete(queueConfig.autoDelete().orElse(false))
                    .arguments(queueConfig.getArguments().orElse(Collections.emptyMap()))
                    .declare();
            management.binding()
                    .sourceExchange(exchangeConfig.getName())
                    .destinationQueue(queueConfig.getName())
                    .key(config.getRoutingKey())
                    .bind();
        } catch (final Exception e) {
            LOGGER.error("Failed to create AMQP topology for exchange '{}', queue '{}', routing key '{}'",
                    config.getExchange(), config.getQueue(), config.getRoutingKey(), e);
            throw e;
        }
    }

    private static RetryTemplate amqpPublishRetryTemplate(final Config.RetryConfig retryConfig) {
        final ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(retryConfig.getInitialInterval().orElse(Duration.ofMillis(500)).toMillis());
        backOffPolicy.setMultiplier(retryConfig.getMultiplier().orElse(2));
        backOffPolicy.setMaxInterval(retryConfig.getMaxInterval().orElse(Duration.ofMinutes(1)).toMillis());

        final RetryTemplateBuilder retryTemplateBuilder = new RetryTemplateBuilder();
        if (retryConfig.getTimeout().isZero()) {
            retryTemplateBuilder.infiniteRetry();
        } else {
            retryTemplateBuilder.withinMillis(retryConfig.getTimeout().toMillis());
        }
        retryTemplateBuilder.customBackoff(backOffPolicy);
        retryTemplateBuilder.withListener(new AmqpRetryLogger());
        return retryTemplateBuilder.build();
    }

    @Override
    public Class<SwimRabbitMQPublisher> getType() {
        return SwimRabbitMQPublisher.class;
    }

    @Override
    public Class<Config> getConfigType() {
        return Config.class;
    }

    public SwimRabbitMQPublisher newInstance(final Config config) {
        requireNonNull(config, "config");
        final Config.ConnectionConfig connectionConfig = config.getConnection();
        final RabbitMQConnectionHealthIndicator connectionHealthIndicator = newConnectionHealthIndicator();
        final RabbitMQPublisherHealthIndicator publisherHealthIndicator = newPublisherHealthIndicator();
        final Environment environment = registerCloseable(newAmqpEnvironmentBuilder().build());

        final AtomicReference<Connection> connectionRef = new AtomicReference<>();
        final AtomicReference<Publisher> publisherRef = new AtomicReference<>();

        final Connection connection = createLazyForwardingProxy(Connection.class, () -> registerCloseable(environment
                .connectionBuilder()
                .username(connectionConfig.getUsername())
                .password(connectionConfig.getPassword())
                .uri(connectionConfig.getUri())
                .listeners(SwimRabbitMQPublisherFactory::log, connectionHealthIndicator)
                .build()), connectionRef);

        final Publisher publisher = createLazyForwardingProxy(Publisher.class, () -> {
            final Config.TopologyConfig topologyConfig = config.getTopology();
            if (topologyConfig.create()) {
                createTopology(connection, topologyConfig);
            }

            return registerCloseable(connection.publisherBuilder()
                    .exchange(topologyConfig.getExchange().getName())
                    .key(topologyConfig.getRoutingKey())
                    .listeners(context -> {
                        if (context.currentState() == Resource.State.CLOSED && context.failureCause() != null) {
                            LOGGER.error("AMQP publisher closed unexpectedly - connection and publisher will be recreated on next publish attempt");
                            unregisterAndClose(connectionRef, "connection");
                            unregisterAndClose(publisherRef, "publisher");
                        }
                    })
                    .build());
        }, publisherRef);

        final SwimRabbitMQPublisher action = newSwimRabbitMQPublisher(publisher, config, publisherHealthIndicator);
        healthContributorRegistry.registerIndicators(config.getId(), connectionHealthIndicator, publisherHealthIndicator);
        return action;
    }

    @VisibleForTesting
    AmqpEnvironmentBuilder newAmqpEnvironmentBuilder() {
        return new AmqpEnvironmentBuilder();
    }

    @VisibleForTesting
    RabbitMQConnectionHealthIndicator newConnectionHealthIndicator() {
        return new RabbitMQConnectionHealthIndicator(clock);
    }

    @VisibleForTesting
    RabbitMQPublisherHealthIndicator newPublisherHealthIndicator() {
        return new RabbitMQPublisherHealthIndicator(clock);
    }

    @VisibleForTesting
    SwimRabbitMQPublisher newSwimRabbitMQPublisher(final Publisher publisher, final Config config,
                                                   final Consumer<Publisher.Context> publisherHealthIndicator) {
        return new SwimRabbitMQPublisher(publisher, config.getPublisherQueueCapacity(),
                config.getPublishTimeout().orElse(Duration.ofSeconds(30)), amqpPublishRetryTemplate(config.getRetry()),
                publisherHealthIndicator);
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

    public interface Config extends ObjectFactoryConfig {
        String getId();

        int getPublisherQueueCapacity();

        Optional<Duration> getPublishTimeout();

        ConnectionConfig getConnection();

        TopologyConfig getTopology();

        RetryConfig getRetry();

        interface ConnectionConfig extends ObjectFactoryConfig {
            String getUri();

            String getUsername();

            String getPassword();
        }

        interface TopologyConfig extends ObjectFactoryConfig {
            boolean create();

            String getRoutingKey();

            ExchangeConfig getExchange();

            QueueConfig getQueue();
        }

        interface ExchangeConfig extends ObjectFactoryConfig {
            String getName();

            Optional<Management.ExchangeType> getType();

            Optional<Boolean> autoDelete();

            Optional<Map<String, Object>> getArguments();
        }

        interface QueueConfig extends ObjectFactoryConfig {
            String getName();

            Optional<Management.QueueType> getType();

            Optional<Boolean> autoDelete();

            Optional<Map<String, Object>> getArguments();
        }

        interface RetryConfig extends ObjectFactoryConfig {
            Optional<Duration> getInitialInterval();

            Optional<Integer> getMultiplier();

            Optional<Duration> getMaxInterval();

            Duration getTimeout();
        }
    }

    private static final class AmqpRetryLogger extends RetryListenerSupport {
        private static final Logger LOGGER = LoggerFactory.getLogger(AmqpRetryLogger.class);

        @Override
        public <T, E extends Throwable> void close(final RetryContext context, final RetryCallback<T, E> callback, @Nullable final Throwable throwable) {
            super.close(context, callback, throwable);
            final int retryCount = context.getRetryCount();
            if (retryCount > 0) {
                if (throwable == null) {
                    LOGGER.info("AMQP publish attempt {} succeeded for <{}>.",
                            loggableValue(RETRY_COUNT_LOGNAME, retryCount + 1), LOGGING_CONTEXT.get(context));
                } else {
                    LOGGER.error("AMQP publish attempts (total {}) exhausted for <{}>.",
                            loggableValue(RETRY_COUNT_LOGNAME, retryCount), LOGGING_CONTEXT.get(context));
                }
            }
        }

        @Override
        public <T, E extends Throwable> void onError(final RetryContext context, final RetryCallback<T, E> callback, final Throwable throwable) {
            super.onError(context, callback, throwable);
            LOGGER.warn("AMQP publish failed on attempt {} for <{}>. Retrying.",
                    loggableValue(RETRY_COUNT_LOGNAME, context.getRetryCount()), LOGGING_CONTEXT.get(context), throwable);
        }
    }

}
