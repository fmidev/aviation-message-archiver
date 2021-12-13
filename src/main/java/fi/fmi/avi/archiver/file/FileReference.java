package fi.fmi.avi.archiver.file;

import org.inferred.freebuilder.FreeBuilder;

@FreeBuilder
public abstract class FileReference {
    FileReference() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public static FileReference create(final String productIdentifier, final String filename) {
        return FileReference.builder()//
                .setProductIdentifier(productIdentifier)//
                .setFilename(filename)//
                .build();
    }

    public abstract String getProductIdentifier();

    public abstract String getFilename();

    public abstract Builder toBuilder();

    @Override
    public String toString() {
        return getProductIdentifier() + ":" + getFilename();
    }

    public static class Builder extends FileReference_Builder {
        Builder() {
        }
    }
}
