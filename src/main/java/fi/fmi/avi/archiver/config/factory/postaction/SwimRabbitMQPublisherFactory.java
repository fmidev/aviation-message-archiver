package fi.fmi.avi.archiver.config.factory.postaction;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Suppliers;
import com.rabbitmq.client.amqp.Connection;
import com.rabbitmq.client.amqp.Environment;
import com.rabbitmq.client.amqp.Publisher;
import com.rabbitmq.client.amqp.Resource;
import com.rabbitmq.client.amqp.impl.AmqpEnvironmentBuilder;
import fi.fmi.avi.archiver.config.model.PostActionFactory;
import fi.fmi.avi.archiver.message.processor.postaction.SwimRabbitMQPublisher;
import fi.fmi.avi.archiver.spring.healthcontributor.RabbitMQConnectionHealthIndicator;
import fi.fmi.avi.archiver.spring.healthcontributor.SwimRabbitMQConnectionHealthContributor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cglib.proxy.Proxy;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public class SwimRabbitMQPublisherFactory implements PostActionFactory<SwimRabbitMQPublisher>, AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(SwimRabbitMQPublisherFactory.class);

    private final SwimRabbitMQConnectionHealthContributor healthContributorRegistry;
    private final Clock clock;
    private final List<AutoCloseable> closeableResources = new ArrayList<>();

    public SwimRabbitMQPublisherFactory(final SwimRabbitMQConnectionHealthContributor healthContributorRegistry, final Clock clock) {
        this.healthContributorRegistry = requireNonNull(healthContributorRegistry, "healthContributorRegistry");
        this.clock = requireNonNull(clock, "clock");
    }

    private static <T> T createLazyForwardingProxy(final Class<T> type, final Supplier<T> factory) {
        final Supplier<T> delegate = Suppliers.memoize(() -> {
            try {
                return factory.get();
            } catch (final RuntimeException exception) {
                LOGGER.error("Could not create instance of {}: {}", type, exception.getMessage(), exception);
                throw exception;
            }
        });
        return type.cast(Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class[]{type},
                (proxy, method, args) -> method.invoke(delegate.get(), args)));
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

    @Override
    public Class<SwimRabbitMQPublisher> getType() {
        return SwimRabbitMQPublisher.class;
    }

    @Override
    public SwimRabbitMQPublisher newInstance(final Map<String, Object> config) {
        // TODO: better handling and arrangement of config, detect unknown config keys
        final String name = config.get("id").toString();
        final String username = config.get("username").toString();
        final String password = config.get("password").toString();
        final String uri = config.get("uri").toString();
        final String exchangeName = config.get("exchange").toString();
        final String routingKey = config.get("routing-key").toString();

        final RabbitMQConnectionHealthIndicator healthIndicator = newHealthIndicator();
        final Environment environment = registerCloseable(newAmqpEnvironmentBuilder().build());
        final Connection connection = createLazyForwardingProxy(Connection.class, () -> registerCloseable(environment
                .connectionBuilder()
                .username(username)
                .password(password)
                .uri(uri)
                .listeners(SwimRabbitMQPublisherFactory::log, healthIndicator)
                .build()));
        final Publisher publisher = createLazyForwardingProxy(Publisher.class, () -> registerCloseable(connection.publisherBuilder()
                .exchange(exchangeName)
                .key(routingKey)
                .build()));
        final SwimRabbitMQPublisher action = newSwimRabbitMQPublisher(publisher);
        healthContributorRegistry.registerContributor(name, healthIndicator);
        return action;
    }

    @VisibleForTesting
    AmqpEnvironmentBuilder newAmqpEnvironmentBuilder() {
        return new AmqpEnvironmentBuilder();
    }

    @VisibleForTesting
    RabbitMQConnectionHealthIndicator newHealthIndicator() {
        return new RabbitMQConnectionHealthIndicator(clock);
    }

    @VisibleForTesting
    SwimRabbitMQPublisher newSwimRabbitMQPublisher(final Publisher publisher) {
        return new SwimRabbitMQPublisher(publisher);
    }

    private <T extends AutoCloseable> T registerCloseable(final T closeableResource) {
        synchronized (closeableResources) {
            closeableResources.add(closeableResource);
        }
        return closeableResource;
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
}
