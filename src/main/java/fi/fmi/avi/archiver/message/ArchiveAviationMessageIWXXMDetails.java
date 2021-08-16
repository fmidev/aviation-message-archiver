package fi.fmi.avi.archiver.message;

import java.util.Optional;

import org.inferred.freebuilder.FreeBuilder;

@FreeBuilder
public abstract class ArchiveAviationMessageIWXXMDetails {
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

    public abstract Optional<String> getXMLNamespace();

    public abstract Optional<String> getCollectIdentifier();

    public abstract Builder toBuilder();

    public static class Builder extends ArchiveAviationMessageIWXXMDetails_Builder {
        Builder() {
        }
    }
}
