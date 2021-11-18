package fi.fmi.avi.archiver.spring.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.Lifecycle;
import org.springframework.context.event.ContextClosedEvent;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings("UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR")
class GracefulShutdownManagerTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(GracefulShutdownManagerTest.class);
    private static Duration pollingInterval = Duration.ofMillis(20);
    private static Duration timeout = pollingInterval.multipliedBy(5);

    private AutoCloseable mocks;
    @Mock
    private Lifecycle taskReception;

    private static GracefulShutdownManager newGracefulShutdownManager(final Lifecycle taskReception, final Supplier<Boolean> tasksRunning) {
        final GracefulShutdownManager shutdownManager = new GracefulShutdownManager(taskReception, tasksRunning);
        shutdownManager.setPollingInterval(pollingInterval);
        shutdownManager.setTimeout(timeout);
        return shutdownManager;
    }

    private static ContextClosedEvent newContextClosedEvent() {
        return new ContextClosedEvent(mock(ApplicationContext.class));
    }

    /**
     * Attempt to estimate appropriate timing assumptions for tests.
     */
    @BeforeAll
    static void estimateTiming() {
        final long start = System.currentTimeMillis();
        for (int i = 0; i < 10; i++) {
            try {
                Thread.sleep(1);
            } catch (final InterruptedException ignored) {
                // continue
            }
        }
        final int iterations = 100_000;
        double sum = 0;
        for (int i = 0; i < iterations; i++) {
            sum += Math.random();
        }
        final long dummy = (long) (sum / iterations); // == 0
        final long finish = System.currentTimeMillis();
        pollingInterval = Duration.ofMillis(finish - start + dummy);
        timeout = pollingInterval.multipliedBy(5);
        LOGGER.info("Using pollingInterval={}; timeout={}", pollingInterval, timeout);
    }

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
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
        final Clock clock = Clock.systemUTC();

        final Instant start = clock.instant();
        shutdownManager.onApplicationEvent(newContextClosedEvent());
        final Instant finish = clock.instant();

        assertThat(Duration.between(start, finish)).isLessThan(pollingInterval);
    }

    @Test
    void onApplicationEvent_waits_until_timeout_if_tasks_dont_stop() {
        final Supplier<Boolean> tasksRunning = () -> true;
        final GracefulShutdownManager shutdownManager = newGracefulShutdownManager(taskReception, tasksRunning);
        final Clock clock = Clock.systemUTC();

        final Instant start = clock.instant();
        shutdownManager.onApplicationEvent(newContextClosedEvent());
        final Instant finish = clock.instant();

        assertThat(Duration.between(start, finish)).isGreaterThanOrEqualTo(timeout);
    }

    @Test
    void onApplicationEvent_waits_until_tasksRunning_returns_false() {
        final Clock clock = Clock.systemUTC();
        final List<Instant> invocations = Collections.synchronizedList(new ArrayList<>());
        final int invocationsUntilFalse = 4;
        final Supplier<Boolean> tasksRunning = () -> {
            invocations.add(clock.instant());
            return invocations.size() < invocationsUntilFalse;
        };
        final GracefulShutdownManager shutdownManager = newGracefulShutdownManager(taskReception, tasksRunning);

        shutdownManager.onApplicationEvent(newContextClosedEvent());
        final Instant finish = clock.instant();

        final int minInvocationsReturningFalse = 1;
        final int maxInvocationsReturningFalse = 2;
        assertThat(invocations)//
                .as("Poll until false")//
                .hasSizeBetween(//
                        invocationsUntilFalse + minInvocationsReturningFalse, //
                        invocationsUntilFalse + maxInvocationsReturningFalse);
        assertThat(Duration.between(invocations.get(invocationsUntilFalse), finish))//
                .as("Not sleeping once tasksRunning returns false")//
                .isLessThan(pollingInterval);
    }
}
