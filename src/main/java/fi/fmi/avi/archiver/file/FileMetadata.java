package fi.fmi.avi.archiver.file;

import org.inferred.freebuilder.FreeBuilder;

import java.time.Instant;
import java.util.Optional;

@FreeBuilder
public abstract class FileMetadata {

    FileMetadata() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public abstract Builder toBuilder();

    public abstract String getFilename();

    public abstract Optional<Instant> getFileModified();

    public abstract FileConfig getFileConfig();

    public abstract String getProductIdentifier();

    public FilenameMatcher createFilenameMatcher() {
        return new FilenameMatcher(getFilename(), getFileConfig().getPattern());
    }

    public static class Builder extends FileMetadata_Builder {
        Builder() {
        }
    }

}
