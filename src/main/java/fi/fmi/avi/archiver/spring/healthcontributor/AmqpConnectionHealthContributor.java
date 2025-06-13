package fi.fmi.avi.archiver.spring.healthcontributor;

import com.rabbitmq.client.amqp.Connection;
import com.rabbitmq.client.amqp.Resource;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Objects.requireNonNull;

public class AmqpConnectionHealthContributor implements HealthIndicator, Connection.StateListener {

    private static final String DETAIL_STATUS = "status";
    private static final String DETAIL_LAST_STATE_CHANGE = "lastStateChange";
    private static final String DETAIL_DURATION = "duration";
    private static final String DETAIL_ERROR = "error";

    private final Clock clock;
    private final AtomicReference<ConnectionState> lastKnownState;

    public AmqpConnectionHealthContributor(final Clock clock) {
        this.clock = requireNonNull(clock, "clock");
        this.lastKnownState = new AtomicReference<>(
                new ConnectionState(Status.UNINITIALIZED, null, clock.instant()));
    }

    @Override
    public void handle(final Resource.Context context) {
        final Status status = switch (context.currentState()) {
            case OPEN -> Status.CONNECTED;
            case RECOVERING -> Status.RECOVERING;
            default -> Status.DISCONNECTED;
        };
        lastKnownState.set(new ConnectionState(status, context.failureCause(), clock.instant()));
    }

    @Override
    public Health health() {
        final ConnectionState state = lastKnownState.get();
        return switch (state.status()) {
            case UNINITIALIZED -> Health.unknown()
                    .withDetail(DETAIL_STATUS, state.status().name())
                    .build();
            case CONNECTED -> Health.up()
                    .withDetail(DETAIL_STATUS, state.status().name())
                    .withDetail(DETAIL_LAST_STATE_CHANGE, state.timestamp())
                    .withDetail(DETAIL_DURATION, Duration.between(state.timestamp(), clock.instant()))
                    .build();
            case RECOVERING, DISCONNECTED -> {
                final Health.Builder builder = Health.down()
                        .withDetail(DETAIL_STATUS, state.status().name())
                        .withDetail(DETAIL_LAST_STATE_CHANGE, state.timestamp())
                        .withDetail(DETAIL_DURATION, Duration.between(state.timestamp(), clock.instant()));
                if (state.failureCause() != null) {
                    builder.withDetail(DETAIL_ERROR, state.failureCause().getMessage());
                }
                yield builder.build();
            }
        };
    }

    private enum Status {
        UNINITIALIZED, CONNECTED, RECOVERING, DISCONNECTED
    }

    private record ConnectionState(Status status, Throwable failureCause, Instant timestamp) {
    }

}