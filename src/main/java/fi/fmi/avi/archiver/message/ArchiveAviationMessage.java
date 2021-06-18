package fi.fmi.avi.archiver.message;

import org.inferred.freebuilder.FreeBuilder;

import java.time.Instant;
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

    public abstract Builder toBuilder();

    public abstract Instant getMessageTime();

    public abstract String getIcaoAirportCode();

    public abstract OptionalInt getStationId();

    public abstract int getType();

    public abstract int getRoute();

    public abstract String getMessage();

    public abstract Optional<Instant> getValidFrom();

    public abstract Optional<Instant> getValidTo();

    public abstract String getHeading();

    public abstract Optional<Instant> getFileModified();

    public abstract Optional<String> getVersion();

    public abstract ProcessingResult getProcessingResult();

    public static class Builder extends ArchiveAviationMessage_Builder {
        Builder() {
            setProcessingResult(ProcessingResult.OK);
        }
    }

}
