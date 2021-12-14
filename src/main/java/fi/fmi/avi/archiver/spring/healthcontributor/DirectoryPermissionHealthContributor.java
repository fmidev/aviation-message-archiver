package fi.fmi.avi.archiver.spring.healthcontributor;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.CompositeHealthContributor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthContributor;
import org.springframework.boot.actuate.health.NamedContributor;

import com.google.common.collect.ImmutableMap;

import fi.fmi.avi.archiver.config.model.AviationProduct;

public class DirectoryPermissionHealthContributor implements CompositeHealthContributor {

    private final Map<String, HealthContributor> healthContributors;
    private final String tempFilePrefix;
    private final String tempFileSuffix;

    public DirectoryPermissionHealthContributor(final Map<String, AviationProduct> aviationProducts, final String tempFilePrefix, final String tempFileSuffix) {
        this.tempFilePrefix = requireNonNull(tempFilePrefix, "tempFilePrefix");
        this.tempFileSuffix = requireNonNull(tempFileSuffix, "tempFileSuffix");
        healthContributors = aviationProducts.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> new ProductDirectoryPermissionHealthContributor(entry.getValue())));
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

    private class ProductDirectoryPermissionHealthContributor implements CompositeHealthContributor {

        private final Map<String, HealthContributor> contributors;

        public ProductDirectoryPermissionHealthContributor(final AviationProduct product) {
            requireNonNull(product, "product");
            contributors = ImmutableMap.of("input (" + product.getInputDir().getPath() + ")",
                    new DirectoryPermissionHealthIndicator(product.getInputDir().toPath()), "archive (" + product.getArchiveDir().getPath() + ")",
                    new DirectoryPermissionHealthIndicator(product.getArchiveDir().toPath()), "fail (" + product.getFailDir().getPath() + ")",
                    new DirectoryPermissionHealthIndicator(product.getFailDir().toPath()));
        }

        @Override
        public HealthContributor getContributor(final String name) {
            requireNonNull(name, "name");
            return contributors.get(name);
        }

        @Override
        public Iterator<NamedContributor<HealthContributor>> iterator() {
            return contributors.entrySet().stream()
                    .map((entry) -> NamedContributor.of(entry.getKey(), entry.getValue())).iterator();
        }

    }

    private class DirectoryPermissionHealthIndicator extends AbstractHealthIndicator {

        private final Path path;

        public DirectoryPermissionHealthIndicator(final Path path) {
            this.path = requireNonNull(path, "path");
        }

        @Override
        protected void doHealthCheck(final Health.Builder builder) {
            Path tempFile = null;
            try {
                tempFile = Files.createTempFile(path, tempFilePrefix, tempFileSuffix);
                if (Files.isReadable(tempFile)) {
                    builder.up();
                } else {
                    builder.down(new AccessDeniedException("Read access denied"));
                }
            } catch (final IOException e) {
                builder.down(e);
            } finally {
                if (tempFile != null) {
                    try {
                        Files.delete(tempFile);
                    } catch (final IOException ignored) {
                        // Nothing to be done here
                    }
                }
            }
        }

    }

}
