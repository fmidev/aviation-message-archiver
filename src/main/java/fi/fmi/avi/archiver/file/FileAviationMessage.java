package fi.fmi.avi.archiver.file;

import fi.fmi.avi.model.Aerodrome;
import fi.fmi.avi.model.MessageType;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.PartialOrCompleteTimePeriod;
import fi.fmi.avi.model.bulletin.BulletinHeading;
import org.inferred.freebuilder.FreeBuilder;

import java.time.Instant;
import java.util.Optional;

/**
 * Model representing content and metadata parsed from a file per message.
 */
@FreeBuilder
public abstract class FileAviationMessage {

    FileAviationMessage() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public abstract Builder toBuilder();

    public abstract Optional<BulletinHeading> getGtsBulletinHeading();

    public abstract Optional<String> getGtsBulletinHeadingString();

    public abstract Optional<BulletinHeading> getCollectIdentifier();

    public abstract Optional<String> getCollectIdentifierString();

    public abstract FilenamePattern getFilenamePattern();

    public abstract String getProductIdentifier();

    public abstract Instant getFileModified();

    public abstract String getContent();

    public abstract MessageType getType();

    public abstract Optional<Aerodrome> getLocationIndicator();

    public abstract Optional<PartialOrCompleteTimeInstant> getMessageTime();

    public abstract Optional<PartialOrCompleteTimePeriod> getValidityPeriod();

    public static class Builder extends FileAviationMessage_Builder {
        Builder() {
        }
    }

}
