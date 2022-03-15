package fi.fmi.avi.archiver.file;

import org.inferred.freebuilder.FreeBuilder;

import fi.fmi.avi.archiver.logging.StructuredLoggable;

@FreeBuilder
public abstract class FileReference implements StructuredLoggable {
    private static final String STRUCTURE_NAME = StructuredLoggable.defaultStructureName(FileReference.class);

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
    public int estimateLogStringLength() {
        return getProductId().length() + 1 + getFilename().length();
    }

    @Override
    public String toString() {
        return getProductId() + "/" + getFilename();
    }

    @Override
    public StructuredLoggable readableCopy() {
        return this;
    }

    @Override
    public String getStructureName() {
        return STRUCTURE_NAME;
    }

    public static class Builder extends FileReference_Builder {
        Builder() {
        }
    }
}
