package fi.fmi.avi.archiver.spring.healthcontributor;

import org.springframework.boot.actuate.health.CompositeHealthContributor;
import org.springframework.boot.actuate.health.HealthContributor;
import org.springframework.boot.actuate.health.NamedContributor;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

public abstract class AbstractMapCompositeHealthContributor implements CompositeHealthContributor {
    protected abstract Map<String, NamedContributor<HealthContributor>> contributors();

    @Nullable
    @Override
    public HealthContributor getContributor(final String name) {
        requireNonNull(name, "name");
        return Optional.ofNullable(contributors().get(name))
                .map(NamedContributor::getContributor)
                .orElse(null);
    }

    @Override
    public Iterator<NamedContributor<HealthContributor>> iterator() {
        return contributors().values().iterator();
    }

    @Override
    public Stream<NamedContributor<HealthContributor>> stream() {
        return contributors().values().stream();
    }
}
