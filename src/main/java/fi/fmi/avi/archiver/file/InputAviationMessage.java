package fi.fmi.avi.archiver.file;

import fi.fmi.avi.model.GenericAviationWeatherMessage;
import org.inferred.freebuilder.FreeBuilder;

import java.util.Optional;

/**
 * Model representing content and metadata parsed from a file per message.
 */
@FreeBuilder
public abstract class InputAviationMessage {

    InputAviationMessage() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public abstract Builder toBuilder();

    public abstract InputBulletinHeading getGtsBulletinHeading();

    public abstract InputBulletinHeading getCollectIdentifier();

    public abstract FileMetadata getFileMetadata();

    public abstract GenericAviationWeatherMessage getMessage();

    public abstract Optional<String> getXMLNamespace();

    public static class Builder extends InputAviationMessage_Builder {
        Builder() {
        }
    }

}
