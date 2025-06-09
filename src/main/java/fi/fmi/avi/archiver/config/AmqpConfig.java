package fi.fmi.avi.archiver.config;

import com.rabbitmq.client.amqp.Connection;
import com.rabbitmq.client.amqp.Environment;
import com.rabbitmq.client.amqp.Publisher;
import com.rabbitmq.client.amqp.impl.AmqpEnvironmentBuilder;
import fi.fmi.avi.archiver.amqp.AmqpService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static java.util.Objects.requireNonNull;

@Configuration
public class AmqpConfig {
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
        return environment.connectionBuilder()
                .username(username)
                .password(password)
                .uri(uri)
                .build();
    }

    @Bean
    public Publisher amqpPublisher(final Connection connection) {
        return connection.publisherBuilder()
                .exchange(exchangeName)
                .key(routingKey)
                .build();
    }

    @Bean
    public AmqpService amqpService(final Publisher amqpPublisher) {
        return new AmqpService(amqpPublisher);
    }

}