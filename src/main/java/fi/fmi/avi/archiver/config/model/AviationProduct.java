package fi.fmi.avi.archiver.config.model;

import static com.google.common.base.Preconditions.checkState;

import java.nio.file.Path;
import java.util.List;

import org.inferred.freebuilder.FreeBuilder;

/**
 * Aviation product configuration model.
 */
@FreeBuilder
public abstract class AviationProduct {
    AviationProduct() {
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Return identifier of this product.
     * The identifier must be unique within all products.
     *
     * @return identifier of this product
     */
    public abstract String getId();

    /**
     * Return the name of the route, through which the messages of this product definition arrive from.
     *
     * @return route name
     */
    public abstract String getRoute();

    /**
     * Return the numeric database identifier of the route, through which the messages of this product definition arrive from.
     *
     * @return route identifier
     */
    public abstract int getRouteId();

    /**
     * Return the input directory of incoming message files.
     * This directory is monitored for new files matching {@link #getFileConfigs() file configuration}, that will be read for archival.
     *
     * @return input directory of incoming message files
     */
    public abstract Path getInputDir();

    /**
     * Return directory for successfully archived / processed files.
     * Files that are processed successfully are moved into this directory after processing, adding the
     * {@link fi.fmi.avi.archiver.file.FileProcessingIdentifier processing identifier} as file name suffix.
     *
     * @return directory for successfully archived / processed files
     */
    public abstract Path getArchiveDir();

    /**
     * Return directory for files that were not processed successfully.
     * If an error has occurred during file processing, it will be moved into this directory instead of {@link #getArchiveDir() archive directory}, adding the
     * {@link fi.fmi.avi.archiver.file.FileProcessingIdentifier processing identifier} as file name suffix.
     *
     * @return directory for files that were not processed successfully
     */
    public abstract Path getFailDir();

    /**
     * Return configurations on input files.
     *
     * @return file configurations
     */
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
