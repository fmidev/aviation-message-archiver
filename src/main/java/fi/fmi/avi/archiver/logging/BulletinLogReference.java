package fi.fmi.avi.archiver.logging;

import java.util.Optional;

import org.inferred.freebuilder.FreeBuilder;

import com.google.common.base.Preconditions;

@FreeBuilder
public abstract class BulletinLogReference extends AbstractAppendingLoggable {
    private static final int HEADING_MAX_LENGTH = 44;
    private static final int BULLETIN_INDEX_LENGTH_ESTIMATE = 3;
    private static final int CHAR_INDEX_LENGTH_ESTIMATE = 7;

    public static Builder builder() {
        return new Builder();
    }

    public abstract int getBulletinIndex();

    public abstract Optional<String> getBulletinHeading();

    public abstract int getCharIndex();

    @Override
    public void appendTo(final StringBuilder builder) {
        builder.append(getBulletinIndex() + 1);
        getBulletinHeading().ifPresent(bulletinHeading -> builder.append('(').append(LoggableUtils.sanitize(bulletinHeading, HEADING_MAX_LENGTH)).append(')'));
        if (getCharIndex() >= 0) {
            builder.append('@').append(getCharIndex() + 1);
        }
    }

    @Override
    protected int estimateLogStringLength() {
        return BULLETIN_INDEX_LENGTH_ESTIMATE + 2 + HEADING_MAX_LENGTH + CHAR_INDEX_LENGTH_ESTIMATE;
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
        public Builder setBulletinIndex(final int bulletinIndex) {
            Preconditions.checkArgument(bulletinIndex >= 0, "bulletinIndex must be non-negative; was: %s", bulletinIndex);
            return super.setBulletinIndex(bulletinIndex);
        }

        @Override
        public Builder setCharIndex(final int charIndex) {
            return super.setCharIndex(normalizeIndex(charIndex));
        }
    }
}
