package fi.fmi.avi.archiver.message;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalInt;

public interface ArchiveAviationMessageOrBuilder {
    ProcessingResult getProcessingResult();

    ArchivalStatus getArchivalStatus();

    int getRoute();

    int getFormat();

    int getType();

    Instant getMessageTime();

    String getStationIcaoCode();

    OptionalInt getStationId();

    Optional<Instant> getValidFrom();

    Optional<Instant> getValidTo();

    Optional<Instant> getFileModified();

    Optional<String> getHeading();

    Optional<String> getVersion();

    ArchiveAviationMessageIWXXMDetailsOrBuilder getIWXXMDetailsOrBuilder();

    String getMessage();
}
