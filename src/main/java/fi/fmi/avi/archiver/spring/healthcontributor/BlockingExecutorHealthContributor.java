package fi.fmi.avi.archiver.spring.healthcontributor;

import fi.fmi.avi.archiver.spring.integration.util.MonitorableCallerBlocksPolicy;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

import static java.util.Objects.requireNonNull;

public class BlockingExecutorHealthContributor extends RegistryCompositeHealthContributor {

    private final Duration timeout;

    public BlockingExecutorHealthContributor(final Duration timeout) {
        this.timeout = requireNonNull(timeout, "timeout");
    }

    public void registerPolicy(final String name, final MonitorableCallerBlocksPolicy policy) {
        requireNonNull(name, "name");
        requireNonNull(policy, "policy");
        registerContributor(name, new BlockingExecutorHealthIndicator(policy));
    }

    private class BlockingExecutorHealthIndicator extends AbstractHealthIndicator {

        private final MonitorableCallerBlocksPolicy policy;

        public BlockingExecutorHealthIndicator(final MonitorableCallerBlocksPolicy policy) {
            this.policy = requireNonNull(policy, "policy");
        }

        @Override
        protected void doHealthCheck(final Health.Builder builder) {
            final Duration blockedDuration = policy.getBlockedDuration();
            if (timeout.compareTo(blockedDuration) > 0) {
                builder.up();
            } else {
                builder.down(new TimeoutException("Timeout (" + timeout + ") exceeded; caller has been blocked for " + blockedDuration));
            }
        }

    }

}
