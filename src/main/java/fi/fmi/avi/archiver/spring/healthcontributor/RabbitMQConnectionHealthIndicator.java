package fi.fmi.avi.archiver.spring.healthcontributor;

import com.rabbitmq.client.amqp.Connection;
import com.rabbitmq.client.amqp.Resource;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

import javax.annotation.Nullable;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import static java.util.Objects.requireNonNull;

public class RabbitMQConnectionHealthIndicator implements HealthIndicator, Connection.StateListener {

    private static final String DETAIL_STATUS = "status";
    private static final String DETAIL_LAST_STATE_CHANGE = "lastStateChange";
    private static final String DETAIL_DURATION = "duration";

    private final Clock clock;
    private volatile ConnectionState lastKnownState;

    public RabbitMQConnectionHealthIndicator(final Clock clock) {
        this.clock = requireNonNull(clock, "clock");
        this.lastKnownState =
                new ConnectionState(Status.UNINITIALIZED, null, clock.instant());
    }

    @Override
    public void handle(final Resource.Context context) {
        final Status status = switch (context.currentState()) {
            case OPEN -> Status.CONNECTED;
            case RECOVERING -> Status.RECOVERING;
            default -> Status.DISCONNECTED;
        };
        lastKnownState = new ConnectionState(status, context.failureCause(), clock.instant());
    }

    @Override
    public Health health() {
        final ConnectionState state = lastKnownState;
        final Health.Builder builder = switch (state.status()) {
            case UNINITIALIZED -> Health.unknown();
            case CONNECTED -> Health.up();
            case RECOVERING, DISCONNECTED -> Health.down();
        };
        builder.withDetail(DETAIL_STATUS, state.status().name())
                .withDetail(DETAIL_LAST_STATE_CHANGE, state.timestamp())
                .withDetail(DETAIL_DURATION, Duration.between(state.timestamp(), clock.instant()));
        if (state.failureCause() != null) {
            builder.withException(state.failureCause());
        }
        return builder.build();
    }

    private enum Status {
        UNINITIALIZED, CONNECTED, RECOVERING, DISCONNECTED
    }

    private record ConnectionState(Status status,
                                   @Nullable Throwable failureCause,
                                   Instant timestamp) {

        public ConnectionState {
            requireNonNull(status, "status");
            requireNonNull(timestamp, "timestamp");
        }
    }

}