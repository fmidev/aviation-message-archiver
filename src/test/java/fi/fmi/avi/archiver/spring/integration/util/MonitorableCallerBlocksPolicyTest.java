package fi.fmi.avi.archiver.spring.integration.util;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

public class MonitorableCallerBlocksPolicyTest {

    private final Clock clock = Clock.systemUTC();

    @Test
    public void test_rejection() throws Exception {
        final ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS,
                new SynchronousQueue<>(), Executors.defaultThreadFactory(), new MonitorableCallerBlocksPolicy(clock, 10));

        final AtomicReference<Throwable> exception = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(1);
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    executor.execute(this);
                } catch (final RejectedExecutionException e) {
                    exception.set(e);
                }
                latch.countDown();
            }
        };

        try {
            executor.execute(runnable);
            assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
            assertThat(exception.get())
                    .isInstanceOf(RejectedExecutionException.class)
                    .hasMessage("Max wait time expired to queue task");
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void test_queue() throws Exception {
        final MonitorableCallerBlocksPolicy policy = new MonitorableCallerBlocksPolicy(clock, Long.MAX_VALUE);
        final ThreadPoolExecutor executor = new ThreadPoolExecutor(2, 2, 0, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(1), Executors.defaultThreadFactory(), policy);
        final AtomicReference<Throwable> exception = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(3);
        try {
            executor.execute(() -> {
                try {
                    final Runnable runnable = () -> {
                        try {
                            Thread.sleep(50);
                            if (latch.getCount() == 3) {
                                assertThat(policy.getBlockedDuration()).isPositive();
                            } else {
                                assertThat(policy.getBlockedDuration()).isZero();
                            }
                        } catch (final InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException(e);
                        } finally {
                            latch.countDown();
                        }
                    };
                    executor.execute(runnable);
                    executor.execute(runnable); // queued
                    executor.execute(runnable); // blocked and queued later
                } catch (final RejectedExecutionException e) {
                    exception.set(e);
                }
            });
            assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
            assertThat(exception.get()).isNull();
        } finally {
            executor.shutdown();
        }
    }

}
