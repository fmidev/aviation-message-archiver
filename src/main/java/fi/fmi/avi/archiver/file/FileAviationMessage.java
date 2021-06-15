package fi.fmi.avi.archiver.file;

import fi.fmi.avi.model.GenericAviationWeatherMessage;
import org.inferred.freebuilder.FreeBuilder;

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

    public abstract FileBulletinHeading getGtsBulletinHeading();

    public abstract FileBulletinHeading getCollectIdentifier();

    public abstract FileMetadata getFileMetadata();

    public abstract GenericAviationWeatherMessage getMessage();

    public static class Builder extends FileAviationMessage_Builder {
        Builder() {
        }
    }

}
