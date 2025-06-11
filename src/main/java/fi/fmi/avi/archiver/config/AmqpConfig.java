package fi.fmi.avi.archiver.config;

import com.rabbitmq.client.amqp.Connection;
import com.rabbitmq.client.amqp.Environment;
import com.rabbitmq.client.amqp.Publisher;
import com.rabbitmq.client.amqp.impl.AmqpEnvironmentBuilder;
import fi.fmi.avi.archiver.amqp.AmqpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cglib.proxy.InvocationHandler;
import org.springframework.cglib.proxy.Proxy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Method;

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

    @Bean
    public Environment amqpEnvironment() {
        return new AmqpEnvironmentBuilder().build();
    }

    @Bean
    public Connection amqpConnection(final Environment environment) {
        return createLazyConnection(environment);
    }

    @Bean
    public Publisher amqpPublisher(final Connection connection) {
        return createLazyPublisher(connection);
    }

    @Bean
    public AmqpService amqpService(final Publisher amqpPublisher) {
        return new AmqpService(amqpPublisher);
    }

    private Connection createLazyConnection(final Environment environment) {
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class[]{Connection.class},
                new LazyConnectionHandler(environment)
        );
    }

    private Publisher createLazyPublisher(final Connection connection) {
        return (Publisher) Proxy.newProxyInstance(
                Publisher.class.getClassLoader(),
                new Class[]{Publisher.class},
                new LazyPublisherHandler(connection)
        );
    }

    private class LazyConnectionHandler implements InvocationHandler {
        private final Environment environment;
        private final Object lock = new Object();
        private volatile Connection actualConnection;

        public LazyConnectionHandler(final Environment environment) {
            this.environment = environment;
        }

        @Override
        public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
            if (actualConnection == null) {
                synchronized (lock) {
                    if (actualConnection == null) {
                        try {
                            actualConnection = environment
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
                    }
                }
            }
            return method.invoke(actualConnection, args);
        }
    }

    private class LazyPublisherHandler implements InvocationHandler {
        private final Connection connection;
        private final Object lock = new Object();
        private volatile Publisher actualPublisher;

        public LazyPublisherHandler(final Connection connection) {
            this.connection = connection;
        }

        @Override
        public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
            if (actualPublisher == null) {
                synchronized (lock) {
                    if (actualPublisher == null) {
                        try {
                            actualPublisher = connection.publisherBuilder()
                                    .exchange(exchangeName)
                                    .key(routingKey)
                                    .build();
                        } catch (final Exception e) {
                            LOGGER.error("Failed to create publisher: {}", e.getMessage());
                            throw new RuntimeException("AMQP publisher unavailable", e);
                        }
                    }
                }
            }

            try {
                return method.invoke(actualPublisher, args);
            } catch (final Exception e) {
                LOGGER.error("Failed to publish message: {}", e.getMessage());
                actualPublisher = null;
                throw e;
            }
        }
    }

}