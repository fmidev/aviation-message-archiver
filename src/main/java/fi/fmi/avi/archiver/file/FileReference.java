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
                .setProductId(productIdentifier)//
                .setFilename(filename)//
                .build();
    }

    public abstract String getProductId();

    public abstract String getFilename();

    public abstract Builder toBuilder();

    @Override
    public String toString() {
        return getProductId() + "/" + getFilename();
    }

    public static class Builder extends FileReference_Builder {
        Builder() {
        }
    }
}
