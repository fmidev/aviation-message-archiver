package fi.fmi.avi.archiver.message;

import org.inferred.freebuilder.FreeBuilder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.OptionalInt;

import static java.util.Objects.requireNonNull;

/**
 * Model representing an aviation message in the database.
 */
@FreeBuilder
public abstract class ArchiveAviationMessage {

    ArchiveAviationMessage() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public abstract MessagePositionInFile getMessagePositionInFile();

    public abstract ProcessingResult getProcessingResult();

    public abstract ArchivalStatus getArchivalStatus();

    public abstract int getRoute();

    public abstract int getFormat();

    public abstract int getType();

    public abstract Instant getMessageTime();

    public abstract String getStationIcaoCode();

    public abstract OptionalInt getStationId();

    public abstract Optional<Instant> getValidFrom();

    public abstract Optional<Instant> getValidTo();

    public abstract Optional<Instant> getFileModified();

    public abstract Optional<String> getHeading();

    public abstract Optional<String> getVersion();

    public abstract ArchiveAviationMessageIWXXMDetails getIWXXMDetails();

    public abstract String getMessage();

    public abstract Builder toBuilder();

    public static class Builder extends ArchiveAviationMessage_Builder {
        Builder() {
            setArchivalStatus(ArchivalStatus.PENDING);
            setProcessingResult(ProcessingResult.OK);
            setMessagePositionInFile(MessagePositionInFile.getInitial());
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
    }
}
