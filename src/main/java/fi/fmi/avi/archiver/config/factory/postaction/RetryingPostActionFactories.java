package fi.fmi.avi.archiver.config.factory.postaction;

import fi.fmi.avi.archiver.message.processor.postaction.AbstractRetryingPostAction;
import fi.fmi.avi.archiver.util.instantiation.ObjectFactoryConfig;

import java.time.Duration;
import java.util.Optional;

public final class RetryingPostActionFactories {

    private RetryingPostActionFactories() {
        throw new AssertionError();
    }

    public interface RetryConfig extends ObjectFactoryConfig {
        Optional<Duration> initialInterval();

        Optional<Integer> multiplier();

        Optional<Duration> maxInterval();

        Duration timeout();
    }

    public interface RetryParamsFactory {
        AbstractRetryingPostAction.RetryParams retryParams(
                final RetryConfig config,
                final String actionName,
                final Duration actionTimeout,
                final int actionQueueCapacity);
    }

}
