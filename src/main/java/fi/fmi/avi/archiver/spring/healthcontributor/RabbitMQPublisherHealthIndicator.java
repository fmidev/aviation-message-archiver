package fi.fmi.avi.archiver.spring.healthcontributor;

import com.rabbitmq.client.amqp.Publisher;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

import javax.annotation.Nullable;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

public class RabbitMQPublisherHealthIndicator implements HealthIndicator, Consumer<Publisher.Context> {

    private static final String DETAIL_PUBLISHER_STATUS = "publisherStatus";
    private static final String DETAIL_LAST_STATE_CHANGE = "lastStateChange";
    private static final String DETAIL_DURATION = "duration";

    private final Clock clock;
    private volatile PublisherState lastKnownState;

    public RabbitMQPublisherHealthIndicator(final Clock clock) {
        this.clock = requireNonNull(clock, "clock");
        this.lastKnownState =
                new PublisherState(State.UNINITIALIZED, null, null, clock.instant());
    }

    @Override
    public void accept(final Publisher.Context context) {
        requireNonNull(context, "context");
        final State state = context.status() == Publisher.Status.ACCEPTED ? State.UP : State.DOWN;
        lastKnownState = new PublisherState(state, context.status(), context.failureCause(), clock.instant());
    }

    @Override
    public Health health() {
        final PublisherState state = lastKnownState;
        final Health.Builder builder = switch (state.state()) {
            case UNINITIALIZED -> Health.unknown();
            case UP -> Health.up();
            case DOWN -> Health.down();
        };
        builder.withDetail(DETAIL_LAST_STATE_CHANGE, state.timestamp())
                .withDetail(DETAIL_DURATION, Duration.between(state.timestamp(), clock.instant()));
        if (state.publisherStatus() != null) {
            builder.withDetail(DETAIL_PUBLISHER_STATUS, state.publisherStatus());
        }
        if (state.failureCause() != null) {
            builder.withException(state.failureCause());
        }
        return builder.build();
    }

    private enum State {
        UNINITIALIZED, UP, DOWN
    }

    private record PublisherState(State state, @Nullable Publisher.Status publisherStatus,
                                  @Nullable Throwable failureCause,
                                  Instant timestamp) {
        public PublisherState {
            requireNonNull(state, "state");
            requireNonNull(timestamp, "timestamp");
        }
    }

}