package fi.fmi.avi.archiver.file;

import org.inferred.freebuilder.FreeBuilder;

import fi.fmi.avi.archiver.message.MessageReference;
import fi.fmi.avi.model.GenericAviationWeatherMessage;

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

    public abstract InputBulletinHeading getGtsBulletinHeading();

    public abstract InputBulletinHeading getCollectIdentifier();

    public abstract FileMetadata getFileMetadata();

    public abstract GenericAviationWeatherMessage getMessage();

    public abstract MessageReference getMessageReference();

    public abstract Builder toBuilder();

    public static class Builder extends InputAviationMessage_Builder {
        Builder() {
            setMessageReference(MessageReference.getInitial());
        }
    }

}
