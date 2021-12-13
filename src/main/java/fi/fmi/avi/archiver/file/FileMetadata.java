package fi.fmi.avi.archiver.file;

import java.time.Instant;
import java.util.Optional;

import org.inferred.freebuilder.FreeBuilder;

@FreeBuilder
public abstract class FileMetadata {

    FileMetadata() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public abstract Builder toBuilder();

    public abstract FileReference getFileReference();

    public abstract Optional<Instant> getFileModified();

    public abstract FileConfig getFileConfig();

    public FilenameMatcher createFilenameMatcher() {
        return new FilenameMatcher(getFileReference().getFilename(), getFileConfig().getPattern(), getFileConfig().getNameTimeZone());
    }

    public static class Builder extends FileMetadata_Builder {
        Builder() {
        }
    }
}
