package fi.fmi.avi.archiver.spring.context;

import static java.util.Objects.requireNonNull;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.Lifecycle;
import org.springframework.context.event.ContextClosedEvent;

public class GracefulShutdownManager implements ApplicationListener<ContextClosedEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GracefulShutdownManager.class);

    private final Lifecycle taskReception;
    private final Supplier<Boolean> tasksRunning;
    private final Clock clock;
    private final Sleeper sleeper;

    private Duration timeout = Duration.ofSeconds(20);
    private Duration pollingInterval = Duration.ofMillis(100);

    public GracefulShutdownManager(final Lifecycle taskReception, final Supplier<Boolean> tasksRunning) {
        this(taskReception, tasksRunning, Clock.systemUTC(), Thread::sleep);
    }

    /**
     * Constructor allowing to inject custom clock and sleep function for testing.
     *
     * @param taskReception
     *         taskReception
     * @param tasksRunning
     *         tasksRunning
     * @param clock
     *         custom clock
     * @param sleeper
     *         custom sleep function
     */
    GracefulShutdownManager(final Lifecycle taskReception, final Supplier<Boolean> tasksRunning, final Clock clock, final Sleeper sleeper) {
        this.taskReception = requireNonNull(taskReception, "taskReception");
        this.tasksRunning = requireNonNull(tasksRunning, "tasksRunning");
        this.clock = requireNonNull(clock, "clock");
        this.sleeper = requireNonNull(sleeper, "sleeper");
    }

    @Override
    public void onApplicationEvent(final ContextClosedEvent event) {
        requireNonNull(event, "event");
        LOGGER.info("Received ContextClosedEvent event at {} for {}", Instant.ofEpochMilli(event.getTimestamp()), event.getSource());
        stop();
        waitWhileTasksRunning();
    }

    private void stop() {
        LOGGER.info("Stopping task reception...");
        taskReception.stop();
        LOGGER.info("Task reception stopped");
    }

    private void waitWhileTasksRunning() {
        if (tasksRunning.get()) {
            LOGGER.info("Waiting for running tasks to finish");
            final long pollingIntervalMillis = pollingInterval.toMillis();
            final long waitUntil = clock.millis() + timeout.toMillis();
            while (tasksRunning.get() && clock.millis() < waitUntil) {
                try {
                    sleeper.sleep(pollingIntervalMillis);
                } catch (final InterruptedException e) {
                    LOGGER.warn("Wait interrupted", e);
                    break;
                }
            }
        }
        if (tasksRunning.get()) {
            LOGGER.warn("Running tasks were not finished within timeout ({})", timeout);
        } else {
            LOGGER.info("No tasks running. Proceeding with shutdown");
        }
    }

    public void setTimeout(final Duration timeout) {
        requireNonNull(timeout, "timeout");
        if (timeout.isNegative()) {
            throw new IllegalArgumentException("timeout must not be negative");
        }
        this.timeout = timeout;
    }

    public void setPollingInterval(final Duration pollingInterval) {
        requireNonNull(pollingInterval, "pollingInterval");
        if (pollingInterval.isNegative() || pollingInterval.isZero()) {
            throw new IllegalArgumentException("pollingInterval must be positive");
        }
        this.pollingInterval = pollingInterval;
    }

    @FunctionalInterface
    interface Sleeper {
        void sleep(long millis) throws InterruptedException;
    }
}
