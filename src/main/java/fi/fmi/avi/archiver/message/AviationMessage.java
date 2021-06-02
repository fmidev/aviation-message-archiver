package fi.fmi.avi.archiver.message;

import jdk.nashorn.internal.ir.annotations.Immutable;
import org.inferred.freebuilder.FreeBuilder;

import java.time.Instant;
import java.util.Optional;

@Immutable
@FreeBuilder
public abstract class AviationMessage {

    AviationMessage() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public abstract Builder toBuilder();

    public abstract Instant getMessageTime();

    public abstract String getIcaoAirportCode();

    public abstract int getType();

    public abstract int getRoute();

    public abstract String getMessage();

    public abstract Optional<Instant> getValidFrom();

    public abstract Optional<Instant> getValidTo();

    public abstract String getHeading();

    public abstract Optional<Instant> getFileModified();

    public abstract Optional<String> getVersion();

    public abstract ProcessingResult getProcessingResult();

    public static class Builder extends AviationMessage_Builder {
        Builder() {
            setProcessingResult(ProcessingResult.OK);
        }
    }

}
