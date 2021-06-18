package fi.fmi.avi.archiver.file;

import org.inferred.freebuilder.FreeBuilder;

import java.time.Instant;

@FreeBuilder
public abstract class FileMetadata {

    FileMetadata() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public abstract Builder toBuilder();

    public abstract FilenamePattern getFilenamePattern();

    public abstract String getProductIdentifier();

    public abstract Instant getFileModified();

    public static class Builder extends FileMetadata_Builder {
        Builder() {
        }
    }

}
