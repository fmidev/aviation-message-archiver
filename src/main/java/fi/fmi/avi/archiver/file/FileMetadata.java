package fi.fmi.avi.archiver.file;

import java.time.Instant;
import java.util.Optional;

import org.inferred.freebuilder.FreeBuilder;

import fi.fmi.avi.archiver.config.model.FileConfig;

/**
 * Model holding metadata of the file under processing.
 */
@FreeBuilder
public abstract class FileMetadata {

    FileMetadata() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public abstract Builder toBuilder();

    /**
     * Return reference to the file this object represents.
     *
     * @return reference to the file this object represents
     */
    public abstract FileReference getFileReference();

    /**
     * Return last modified time of the file, if available.
     *
     * @return last modified time of the file, if available, otherwise empty
     */
    public abstract Optional<Instant> getFileModified();

    /**
     * Return product file config matching the file this object represents.
     *
     * @return product file config matching the file this object represents
     */
    public abstract FileConfig getFileConfig();

    /**
     * Return a new {@code FilenameMatcher} on the file this object represents.
     *
     * @return new {@code FilenameMatcher} on the file this object represents
     */
    public FilenameMatcher createFilenameMatcher() {
        return new FilenameMatcher(getFileReference().getFilename(), getFileConfig().getPattern(), getFileConfig().getNameTimeZone());
    }

    public static class Builder extends FileMetadata_Builder {
        Builder() {
        }
    }
}
