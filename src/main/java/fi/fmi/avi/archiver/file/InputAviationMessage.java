package fi.fmi.avi.archiver.file;

import fi.fmi.avi.archiver.message.MessagePositionInFile;
import fi.fmi.avi.model.GenericAviationWeatherMessage;
import org.inferred.freebuilder.FreeBuilder;

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

    /**
     * Return GTS bulletin heading.
     *
     * @return GTS bulletin heading
     */
    public abstract InputBulletinHeading getGtsBulletinHeading();

    /**
     * Return COLLECT document identifier.
     *
     * @return COLLECT document identifier
     */
    public abstract InputBulletinHeading getCollectIdentifier();


    /**
     * Return metadata of the file this message belongs to.
     *
     * @return metadata of the file this message belongs to
     */
    public abstract FileMetadata getFileMetadata();

    /**
     * Return message position within the file this message belongs to.
     *
     * @return message position within the file this message belongs to
     */
    public abstract MessagePositionInFile getMessagePositionInFile();

    /**
     * Return the parsed message.
     *
     * @return the parsed message
     */
    public abstract GenericAviationWeatherMessage getMessage();

    public abstract Builder toBuilder();

    public static class Builder extends InputAviationMessage_Builder {
        Builder() {
            setMessagePositionInFile(MessagePositionInFile.getInitial());
        }
    }
}
