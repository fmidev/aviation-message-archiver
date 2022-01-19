package fi.fmi.avi.archiver.config.model;

import static com.google.common.base.Preconditions.checkState;

import java.nio.file.Path;
import java.util.List;

import org.inferred.freebuilder.FreeBuilder;

@FreeBuilder
public abstract class AviationProduct {
    AviationProduct() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public abstract String getId();

    public abstract String getRoute();

    public abstract int getRouteId();

    public abstract Path getInputDir();

    public abstract Path getArchiveDir();

    public abstract Path getFailDir();

    public abstract List<FileConfig> getFileConfigs();

    public abstract Builder toBuilder();

    public static class Builder extends AviationProduct_Builder {
        Builder() {
        }

        @Override
        public AviationProduct build() {
            checkState(!getId().isEmpty(), "id is empty");
            checkState(!getRoute().isEmpty(), "route is empty");
            checkState(!getBuildersOfFileConfigs().isEmpty(), "fileConfigs (files) is empty");
            return super.build();
        }

        public List<FileConfig.Builder> getFiles() {
            return getBuildersOfFileConfigs();
        }

        public Builder setFiles(final List<FileConfig.Builder> files) {
            return clearFileConfigs().addAllBuildersOfFileConfigs(files);
        }
    }
}
