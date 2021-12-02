package fi.fmi.avi.archiver.spring.healthcontributor;

import fi.fmi.avi.archiver.spring.integration.util.MonitorableCallerBlocksPolicy;
import org.springframework.boot.actuate.health.*;

import java.time.Duration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static java.util.Objects.requireNonNull;

public class BlockingExecutorHealthContributor implements CompositeHealthContributor {

    private final Duration timeout;
    private final Map<String, HealthContributor> healthContributors = new HashMap<>();

    public BlockingExecutorHealthContributor(final Duration timeout) {
        this.timeout = requireNonNull(timeout, "timeout");
    }

    public void register(final String name, final MonitorableCallerBlocksPolicy policy) {
        requireNonNull(name, "name");
        requireNonNull(policy, "policy");
        healthContributors.put(name, new BlockingExecutorHealthIndicator(policy));
    }

    @Override
    public HealthContributor getContributor(final String name) {
        requireNonNull(name, "name");
        return healthContributors.get(name);
    }

    @Override
    public Iterator<NamedContributor<HealthContributor>> iterator() {
        return healthContributors.entrySet().stream()
                .map((entry) -> NamedContributor.of(entry.getKey(), entry.getValue())).iterator();
    }

    private class BlockingExecutorHealthIndicator extends AbstractHealthIndicator {

        private final MonitorableCallerBlocksPolicy policy;

        public BlockingExecutorHealthIndicator(final MonitorableCallerBlocksPolicy policy) {
            this.policy = requireNonNull(policy, "policy");
        }

        @Override
        protected void doHealthCheck(final Health.Builder builder) {
            final Duration blockedDuration = policy.getBlockedDuration();
            if (blockedDuration.minus(timeout).isNegative()) {
                builder.up();
            } else {
                builder.down(new TimeoutException("Caller has been blocked for " + blockedDuration));
            }
        }

    }

}
