package fi.fmi.avi.archiver.spring.healthcontributor;

import fi.fmi.avi.archiver.config.model.AviationProduct;
import org.springframework.boot.actuate.health.*;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

public class DirectoryPermissionHealthContributor extends AbstractMapCompositeHealthContributor {

    private final Map<String, NamedContributor<HealthContributor>> healthContributors;
    private final String tempFilePrefix;
    private final String tempFileSuffix;

    public DirectoryPermissionHealthContributor(final Map<String, AviationProduct> aviationProducts, final String tempFilePrefix, final String tempFileSuffix) {
        this.tempFilePrefix = requireNonNull(tempFilePrefix, "tempFilePrefix");
        this.tempFileSuffix = requireNonNull(tempFileSuffix, "tempFileSuffix");
        healthContributors = aviationProducts.values()
                .stream()
                .collect(Collectors.toMap(AviationProduct::getId, product -> NamedContributor.of(
                        product.getId(),
                        CompositeHealthContributor.fromMap(Map.of(//
                                "input", new DirectoryPermissionHealthIndicator(product.getInputDir()), //
                                "archive", new DirectoryPermissionHealthIndicator(product.getArchiveDir()), //
                                "fail", new DirectoryPermissionHealthIndicator(product.getFailDir())//
                        )))));
    }

    @Override
    protected Map<String, NamedContributor<HealthContributor>> contributors() {
        return healthContributors;
    }

    private class DirectoryPermissionHealthIndicator extends AbstractHealthIndicator {

        private final Path path;

        public DirectoryPermissionHealthIndicator(final Path path) {
            this.path = requireNonNull(path, "path");
        }

        @Override
        protected void doHealthCheck(final Health.Builder builder) {
            builder.withDetail("path", path);
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
