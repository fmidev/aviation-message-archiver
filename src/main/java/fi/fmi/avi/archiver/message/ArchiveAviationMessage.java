package fi.fmi.avi.archiver.message;

import org.inferred.freebuilder.FreeBuilder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static java.util.Objects.requireNonNull;

/**
 * Model representing an aviation message in the database.
 */
@FreeBuilder
public abstract class ArchiveAviationMessage implements ArchiveAviationMessageOrBuilder {

    ArchiveAviationMessage() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public abstract ArchiveAviationMessageIWXXMDetails getIWXXMDetails();

    @Override
    public ArchiveAviationMessageIWXXMDetailsOrBuilder getIWXXMDetailsOrBuilder() {
        return getIWXXMDetails();
    }

    public abstract Builder toBuilder();

    public static class Builder extends ArchiveAviationMessage_Builder implements ArchiveAviationMessageOrBuilder {
        Builder() {
            setArchivalStatus(ArchivalStatus.PENDING);
            setProcessingResult(ProcessingResult.OK);
        }

        /**
         * Truncates the given instant to microseconds to ensure consistency with database that stores
         * timestamps at microsecond precision.
         *
         * @param instant the instant to truncate
         * @return the truncated instant
         */
        private Instant truncateToMicros(final Instant instant) {
            return instant.truncatedTo(ChronoUnit.MICROS);
        }

        @Override
        public Builder setMessageTime(final Instant messageTime) {
            return super.setMessageTime(truncateToMicros(requireNonNull(messageTime, "messageTime")));
        }

        @Override
        public Builder setValidFrom(final Instant validFrom) {
            return super.setValidFrom(truncateToMicros(requireNonNull(validFrom, "validFrom")));
        }

        @Override
        public Builder setValidTo(final Instant validTo) {
            return super.setValidTo(truncateToMicros(requireNonNull(validTo, "validTo")));
        }

        @Override
        public Builder setFileModified(final Instant fileModified) {
            return super.setFileModified(truncateToMicros(requireNonNull(fileModified, "fileModified")));
        }

        @Override
        public ArchiveAviationMessageIWXXMDetailsOrBuilder getIWXXMDetailsOrBuilder() {
            return getIWXXMDetailsBuilder();
        }
    }
}
