package fi.fmi.avi.archiver.config;

import com.google.common.base.Suppliers;
import com.rabbitmq.client.amqp.Connection;
import com.rabbitmq.client.amqp.Environment;
import com.rabbitmq.client.amqp.Publisher;
import com.rabbitmq.client.amqp.impl.AmqpEnvironmentBuilder;
import fi.fmi.avi.archiver.amqp.AmqpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cglib.proxy.Proxy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

@Configuration
public class AmqpConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmqpConfig.class);

    private final String uri;
    private final String username;
    private final String password;
    private final String exchangeName;
    private final String routingKey;

    public AmqpConfig(
            @Value("${amqp.uri}") final String uri,
            @Value("${amqp.username}") final String username,
            @Value("${amqp.password}") final String password,
            @Value("${amqp.exchange}") final String exchangeName,
            @Value("${amqp.routing-key}") final String routingKey) {
        this.uri = requireNonNull(uri, "uri");
        this.username = requireNonNull(username, "username");
        this.password = requireNonNull(password, "password");
        this.exchangeName = requireNonNull(exchangeName, "exchangeName");
        this.routingKey = requireNonNull(routingKey, "routingKey");
    }

    private static <T> T createLazyForwardingProxy(final Class<T> type, final Supplier<T> factory) {
        final com.google.common.base.Supplier<T> delegate = Suppliers.memoize(factory::get);
        return type.cast(Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class[]{type},
                (proxy, method, args) -> method.invoke(delegate.get(), args)));
    }

    @Bean
    public Environment amqpEnvironment() {
        return new AmqpEnvironmentBuilder().build();
    }

    @Bean
    public Connection amqpConnection(final Environment environment) {
        return createLazyForwardingProxy(Connection.class, () -> {
            try {
                return environment
                        .connectionBuilder()
                        .username(username)
                        .password(password)
                        .uri(uri)
                        .listeners(context -> {
                            switch (context.currentState()) {
                                case OPEN -> LOGGER.info("RabbitMQ connection established");
                                case RECOVERING -> LOGGER.info("RabbitMQ connection recovering...");
                                case CLOSED -> {
                                    if (context.failureCause() != null) {
                                        LOGGER.error("Connection lost: {}", context.failureCause().getMessage());
                                    }
                                }
                            }
                        })
                        .build();
            } catch (final Exception e) {
                throw new RuntimeException("Failed to establish RabbitMQ connection", e);
            }
        });
    }

    @Bean
    public Publisher amqpPublisher(final Connection connection) {
        return createLazyForwardingProxy(Publisher.class, () -> {
            try {
                return connection.publisherBuilder()
                        .exchange(exchangeName)
                        .key(routingKey)
                        .build();
            } catch (final Exception e) {
                LOGGER.error("Failed to create publisher: {}", e.getMessage());
                throw new RuntimeException("AMQP publisher unavailable", e);
            }
        });
    }

    @Bean
    public AmqpService amqpService(final Publisher amqpPublisher) {
        return new AmqpService(amqpPublisher);
    }

}
