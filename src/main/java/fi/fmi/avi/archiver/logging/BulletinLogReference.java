package fi.fmi.avi.archiver.logging;

import java.util.Optional;

import org.inferred.freebuilder.FreeBuilder;

import com.google.common.base.Preconditions;

@FreeBuilder
public abstract class BulletinLogReference extends AbstractMemoizingStructuredLoggable {
    private static final String STRUCTURE_NAME = StructuredLoggables.defaultStructureName(BulletinLogReference.class);
    private static final int HEADING_MAX_LENGTH = 44;
    private static final int BULLETIN_INDEX_LENGTH_ESTIMATE = 3;
    private static final int CHAR_INDEX_LENGTH_ESTIMATE = 7;

    public static Builder builder() {
        return new Builder();
    }

    public abstract int getIndex();

    public abstract Optional<String> getHeading();

    public abstract int getCharIndex();

    @Override
    protected void appendOnceTo(final StringBuilder builder) {
        builder.append(getIndex() + 1);
        getHeading().ifPresent(heading -> builder.append('(').append(LoggableUtils.sanitize(heading, HEADING_MAX_LENGTH)).append(')'));
        if (getCharIndex() >= 0) {
            builder.append('@').append(getCharIndex() + 1);
        }
    }

    @Override
    public int estimateLogStringLength() {
        return BULLETIN_INDEX_LENGTH_ESTIMATE + 2 + HEADING_MAX_LENGTH + CHAR_INDEX_LENGTH_ESTIMATE;
    }

    @Override
    public String getStructureName() {
        return STRUCTURE_NAME;
    }

    public abstract Builder toBuilder();

    public static class Builder extends BulletinLogReference_Builder {
        Builder() {
            setCharIndex(-1);
        }

        private static int normalizeIndex(final int index) {
            return Math.max(-1, index);
        }

        @Override
        public Builder setIndex(final int index) {
            Preconditions.checkArgument(index >= 0, "index must be non-negative; was: %s", index);
            return super.setIndex(index);
        }

        @Override
        public Builder setCharIndex(final int charIndex) {
            return super.setCharIndex(normalizeIndex(charIndex));
        }
    }
}
