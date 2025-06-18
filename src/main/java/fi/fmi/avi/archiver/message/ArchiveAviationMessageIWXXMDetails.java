package fi.fmi.avi.archiver.message;

import org.inferred.freebuilder.FreeBuilder;

@FreeBuilder
public abstract class ArchiveAviationMessageIWXXMDetails implements ArchiveAviationMessageIWXXMDetailsOrBuilder {
    private static final ArchiveAviationMessageIWXXMDetails EMPTY = builder().build();
    private static final ArchiveAviationMessageIWXXMDetails EMPTY_PARTIAL = builder().buildPartial();

    ArchiveAviationMessageIWXXMDetails() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isEmpty() {
        return equals(EMPTY) || equals(EMPTY_PARTIAL);
    }

    public abstract Builder toBuilder();

    public static class Builder extends ArchiveAviationMessageIWXXMDetails_Builder implements ArchiveAviationMessageIWXXMDetailsOrBuilder {
        Builder() {
        }
    }
}
