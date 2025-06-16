package fi.fmi.avi.archiver.message;

import org.inferred.freebuilder.FreeBuilder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.OptionalInt;

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

        private static Instant truncateToMicros(final Instant instant) {
            return instant != null ? instant.truncatedTo(ChronoUnit.MICROS) : null;
        }

        @Override
        public ArchiveAviationMessage build() {
            // Truncate Instant fields to microsecond precision to ensure consistency with database
            setMessageTime(getMessageTime().truncatedTo(ChronoUnit.MICROS));
            getValidFrom().ifPresent(instant -> setValidFrom(instant.truncatedTo(ChronoUnit.MICROS)));
            getValidTo().ifPresent(instant -> setValidTo(instant.truncatedTo(ChronoUnit.MICROS)));
            getFileModified().ifPresent(instant -> setFileModified(instant.truncatedTo(ChronoUnit.MICROS)));
            return super.build();
        }
    }
}
