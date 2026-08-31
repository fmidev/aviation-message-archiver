package fi.fmi.avi.archiver.spring.healthcontributor;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.actuate.health.ContributorRegistry;
import org.springframework.boot.actuate.health.HealthContributor;
import org.springframework.boot.actuate.health.NamedContributor;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.util.Objects.requireNonNull;

public class RegistryCompositeHealthContributor extends AbstractMapCompositeHealthContributor implements ContributorRegistry<HealthContributor> {
    private final ConcurrentMap<String, NamedContributor<HealthContributor>> healthContributors = new ConcurrentHashMap<>();

    @Override
    protected Map<String, NamedContributor<HealthContributor>> contributors() {
        return healthContributors;
    }

    @Override
    public void registerContributor(final String name, final HealthContributor contributor) {
        requireNonNull(name, "name");
        requireNonNull(contributor, "contributor");
        final NamedContributor<HealthContributor> previousValue = healthContributors.putIfAbsent(name, NamedContributor.of(name, contributor));
        if (previousValue != null) {
            throw new IllegalStateException("A health contributor already registered with name: " + name);
        }
    }

    @Override
    public @Nullable HealthContributor unregisterContributor(final String name) {
        requireNonNull(name, "name");
        return Optional.of(healthContributors.remove(name))
                .map(NamedContributor::getContributor)
                .orElse(null);
    }
}
