package fi.fmi.avi.archiver.spring.context;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.springframework.context.Lifecycle;

public class CompoundLifecycle implements Lifecycle {
    private final Object monitor = new Object();
    private final List<Lifecycle> lifecycles = new ArrayList<>();

    private boolean traversingLifecycles;

    private void doSynchronized(final Runnable action) {
        synchronized (monitor) {
            if (traversingLifecycles) {
                // Prevent cyclic loops
                return;
            }
            traversingLifecycles = true;
            action.run();
            traversingLifecycles = false;
        }
    }

    private <T> T doSynchronized(final T defaultValue, final Supplier<T> action) {
        synchronized (monitor) {
            if (traversingLifecycles) {
                // Prevent cyclic loops
                return defaultValue;
            }
            traversingLifecycles = true;
            final T result = action.get();
            traversingLifecycles = false;
            return result;
        }
    }

    @Override
    public void start() {
        doSynchronized(this::startUnsafe);
    }

    private void startUnsafe() {
        lifecycles.forEach(Lifecycle::start);
    }

    @Override
    public void stop() {
        doSynchronized(this::stopUnsafe);
    }

    private void stopUnsafe() {
        lifecycles.forEach(Lifecycle::stop);
    }

    @Override
    public boolean isRunning() {
        return isAllRunning();
    }

    public boolean isAllRunning() {
        return doSynchronized(true, this::isAllRunningUnsafe);
    }

    private boolean isAllRunningUnsafe() {
        return lifecycles.stream().allMatch(Lifecycle::isRunning);
    }

    public boolean isAnyRunning() {
        return doSynchronized(false, this::isAnyRunningUnsafe);
    }

    private boolean isAnyRunningUnsafe() {
        return lifecycles.stream().anyMatch(Lifecycle::isRunning);
    }

    public void add(final Lifecycle lifecycle) {
        requireNonNull(lifecycle, "lifecycle");
        synchronized (monitor) {
            lifecycles.add(lifecycle);
        }
    }
}
