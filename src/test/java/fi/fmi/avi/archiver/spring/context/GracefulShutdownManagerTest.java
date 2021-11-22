package fi.fmi.avi.archiver.spring.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationContext;
import org.springframework.context.Lifecycle;
import org.springframework.context.event.ContextClosedEvent;
import org.threeten.extra.MutableClock;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings("UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR")
class GracefulShutdownManagerTest {
    private static final Duration POLLING_INTERVAL = Duration.ofMillis(2);
    private static final Duration TIMEOUT = POLLING_INTERVAL.multipliedBy(5).plus(Duration.ofMillis(1));

    private MutableClock clock;

    private AutoCloseable mocks;
    @Mock
    private Lifecycle taskReception;

    private static ContextClosedEvent newContextClosedEvent() {
        return new ContextClosedEvent(mock(ApplicationContext.class));
    }

    private GracefulShutdownManager newGracefulShutdownManager(final Lifecycle taskReception, final Supplier<Boolean> tasksRunning) {
        return newGracefulShutdownManager(taskReception, tasksRunning, this::fakeSleep);
    }

    private GracefulShutdownManager newGracefulShutdownManager(final Lifecycle taskReception, final Supplier<Boolean> tasksRunning,
            final GracefulShutdownManager.Sleeper sleeper) {
        final GracefulShutdownManager shutdownManager = new GracefulShutdownManager(taskReception, tasksRunning, clock, sleeper);
        shutdownManager.setPollingInterval(POLLING_INTERVAL);
        shutdownManager.setTimeout(TIMEOUT);
        return shutdownManager;
    }

    private void fakeSleep(final long pollingIntervalMillis) {
        clock.add(pollingIntervalMillis, ChronoUnit.MILLIS);
    }

    private Duration time(final Runnable task) {
        final Instant start = clock.instant();
        task.run();
        final Instant finish = clock.instant();
        return Duration.between(start, finish);
    }

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        clock = MutableClock.epochUTC();
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    @Test
    void setTimeout_rejects_negative_values() {
        final GracefulShutdownManager shutdownManager = new GracefulShutdownManager(taskReception, () -> true);
        assertThatIllegalArgumentException().isThrownBy(() -> shutdownManager.setTimeout(Duration.ofMillis(-1)));
    }

    @Test
    void setPollingInterval_rejects_zero_values() {
        final GracefulShutdownManager shutdownManager = new GracefulShutdownManager(taskReception, () -> true);
        assertThatIllegalArgumentException().isThrownBy(() -> shutdownManager.setPollingInterval(Duration.ZERO));
    }

    @Test
    void setPollingInterval_rejects_negative_values() {
        final GracefulShutdownManager shutdownManager = new GracefulShutdownManager(taskReception, () -> true);
        assertThatIllegalArgumentException().isThrownBy(() -> shutdownManager.setPollingInterval(Duration.ofMillis(-1)));
    }

    @Test
    void setPollingInterval_accepts_positive_values() {
        final GracefulShutdownManager shutdownManager = newGracefulShutdownManager(taskReception, () -> true);
        final Duration pollingInterval = Duration.ofMillis(1);

        shutdownManager.setPollingInterval(pollingInterval);
        shutdownManager.setTimeout(pollingInterval);
        final Duration executionTime = time(() -> shutdownManager.onApplicationEvent(newContextClosedEvent()));

        assertThat(executionTime).isEqualTo(pollingInterval);
    }

    @Test
    void onApplicationEvent_invokes_taskReception_stop() {
        final Supplier<Boolean> tasksRunning = () -> false;
        final GracefulShutdownManager shutdownManager = newGracefulShutdownManager(taskReception, tasksRunning);

        shutdownManager.onApplicationEvent(newContextClosedEvent());

        verify(taskReception).stop();
    }

    @Test
    void onApplicationEvent_doesnt_sleep_if_tasksRunning_returns_false_immediately() {
        final Supplier<Boolean> tasksRunning = () -> false;
        final GracefulShutdownManager shutdownManager = newGracefulShutdownManager(taskReception, tasksRunning);

        final Duration executionTime = time(() -> shutdownManager.onApplicationEvent(newContextClosedEvent()));

        assertThat(executionTime).isZero();
    }

    @Test
    void onApplicationEvent_doesnt_sleep_if_timeout_is_zero() {
        final Supplier<Boolean> tasksRunning = () -> true;
        final GracefulShutdownManager shutdownManager = newGracefulShutdownManager(taskReception, tasksRunning);
        shutdownManager.setTimeout(Duration.ZERO);

        final Duration executionTime = time(() -> shutdownManager.onApplicationEvent(newContextClosedEvent()));

        assertThat(executionTime).isZero();
    }

    @Test
    void onApplicationEvent_waits_until_timeout_if_tasks_dont_stop() {
        final Supplier<Boolean> tasksRunning = () -> true;
        final GracefulShutdownManager shutdownManager = newGracefulShutdownManager(taskReception, tasksRunning);

        final Duration executionTime = time(() -> shutdownManager.onApplicationEvent(newContextClosedEvent()));

        assertThat(executionTime).isBetween(TIMEOUT, TIMEOUT.plus(POLLING_INTERVAL));
    }

    @Test
    void onApplicationEvent_waits_until_tasksRunning_returns_false() {
        final int sleepPeriodsWhileRunning = 2;
        final AtomicInteger remainingSleepPeriods = new AtomicInteger(sleepPeriodsWhileRunning);
        final Supplier<Boolean> tasksRunning = () -> remainingSleepPeriods.get() > 0;
        final GracefulShutdownManager.Sleeper sleeper = millis -> {
            fakeSleep(millis);
            remainingSleepPeriods.decrementAndGet();
        };
        final GracefulShutdownManager shutdownManager = newGracefulShutdownManager(taskReception, tasksRunning, sleeper);

        final Duration executionTime = time(() -> shutdownManager.onApplicationEvent(newContextClosedEvent()));

        assertThat(executionTime).isEqualTo(POLLING_INTERVAL.multipliedBy(sleepPeriodsWhileRunning));
    }

    @Test
    void onApplicationEvent_stops_waiting_if_interrupted() {
        final int sleepPeriodsWhileRunning = 2;
        final AtomicInteger remainingSleepPeriods = new AtomicInteger(sleepPeriodsWhileRunning);
        final Supplier<Boolean> tasksRunning = () -> true;
        final GracefulShutdownManager.Sleeper sleeper = millis -> {
            final int remaining = remainingSleepPeriods.getAndDecrement();
            if (remaining > 0) {
                fakeSleep(millis);
            } else {
                throw new InterruptedException("Testing interruption");
            }
        };
        final GracefulShutdownManager shutdownManager = newGracefulShutdownManager(taskReception, tasksRunning, sleeper);

        final Duration executionTime = time(() -> shutdownManager.onApplicationEvent(newContextClosedEvent()));

        assertThat(executionTime).isEqualTo(POLLING_INTERVAL.multipliedBy(sleepPeriodsWhileRunning));
    }
}
