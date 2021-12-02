package fi.fmi.avi.archiver.spring.integration.util;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.Objects.requireNonNull;

/**
 * {@link RejectedExecutionHandler} similar to {@link org.springframework.integration.util.CallerBlocksPolicy} that
 * also allows querying the blocked duration.
 */
public class MonitorableCallerBlocksPolicy implements RejectedExecutionHandler {

    private final Clock clock;
    private final long maxWait;
    private final AtomicLong blockStartMillis = new AtomicLong(-1);

    /**
     * Construct instance based on the provided maximum wait time.
     *
     * @param clock   clock
     * @param maxWait the maximum time to wait for a queue slot to be available, in milliseconds.
     */
    public MonitorableCallerBlocksPolicy(final Clock clock, long maxWait) {
        this.clock = requireNonNull(clock, "clock");
        this.maxWait = maxWait;
    }

    @Override
    public void rejectedExecution(final Runnable runnable, final ThreadPoolExecutor executor) {
        if (!executor.isShutdown()) {
            try {
                final BlockingQueue<Runnable> queue = executor.getQueue();
                blockStartMillis.set(clock.millis());
                if (!queue.offer(runnable, this.maxWait, TimeUnit.MILLISECONDS)) {
                    throw new RejectedExecutionException("Max wait time expired to queue task");
                }
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RejectedExecutionException("Interrupted", e);
            } finally {
                blockStartMillis.set(-1);
            }
        } else {
            throw new RejectedExecutionException("Executor has been shut down");
        }
    }

    public Duration getBlockedDuration() {
        final long millis = blockStartMillis.get();
        if (millis > -1) {
            return Duration.between(Instant.ofEpochMilli(millis), clock.instant());
        } else {
            return Duration.ZERO;
        }
    }

}
