package fi.fmi.avi.archiver.logging.model;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import fi.fmi.avi.archiver.file.FileReference;
import fi.fmi.avi.archiver.logging.AppendingLoggable;
import fi.fmi.avi.archiver.logging.StructuredLoggable;

/**
 * Loggable context information on processing a file of aviation messages.
 * It provides information on file, bulletin within the file and message within the bulletin being under processing, and produces loggable output of this
 * information.
 *
 * <p>
 * All bulletin and message indices start from {@code 0}.
 * </p>
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public interface ReadableLoggingContext extends AppendingLoggable, StructuredLoggable {
    /**
     * Default structure name, in terms of {@link StructuredLoggable#getStructureName()}.
     */
    String DEFAULT_STRUCTURE_NAME = "processingContext";

    /**
     * {@inheritDoc}
     *
     * <p>
     * The default implementation returns an immutable {@code ReadableLoggingContext}.
     * </p>
     *
     * @return {@inheritDoc}
     */
    @Override
    default ReadableLoggingContext readableCopy() {
        return ImmutableLoggingContext.copyOf(this);
    }

    /**
     * Returns default name for this structured object. When no other name for a structured loggable object is provided by context, this name can be used.
     *
     * <p>
     * The default implementation returns always {@value #DEFAULT_STRUCTURE_NAME}.
     * </p>
     *
     * @return {@inheritDoc}
     */
    @Override
    default String getStructureName() {
        return DEFAULT_STRUCTURE_NAME;
    }

    /**
     * Return {@code FileReference} referring to the file currently under processing, if one is registered, otherwise empty.
     *
     * @return {@code FileReference} referring to the file currently under processing, if one is registered, otherwise empty
     */
    Optional<FileReference> getFile();

    /**
     * Return a {@code BulletinLogReference} referring to the bulletin currently under processing, if one is registered, otherwise empty.
     *
     * @return a {@code BulletinLogReference} referring to the bulletin currently under processing, if one is registered, otherwise empty.
     */
    Optional<BulletinLogReference> getBulletin();

    /**
     * Return the index of the currently processed bulletin within a file starting from index {@code 0}.
     * Returns {@code -1} if no bulletin is currently under processing.
     *
     * <p>
     * The default implementation returns the bulletin index of {@code BulletinLogReference} returned by {@link #getBulletin()}.
     * </p>
     *
     * @return index of bulletin currently under processing, or {@code -1}
     */
    @JsonIgnore
    default int getBulletinIndex() {
        return getBulletin()//
                .map(BulletinLogReference::getIndex)//
                .orElse(-1);
    }

    /**
     * Return a {@code MessageLogReference} referring to the message being currently processed, if one is registered, otherwise empty.
     *
     * @return {@code MessageLogReference} referring to message being currently under processing, if one is registered, otherwise empty
     */
    Optional<MessageLogReference> getMessage();

    /**
     * Return the index of the currently processed message within a bulletin starting from index {@code 0}.
     * Returns {@code -1} if no message is currently under processing.
     *
     * <p>
     * The default implementation returns the message index of {@code MessageLogReference} returned by {@link #getMessage()}.
     * </p>
     *
     * @return index of message being currently under processing, or {@code -1}
     */
    @JsonIgnore
    default int getMessageIndex() {
        return getMessage()//
                .map(MessageLogReference::getIndex)//
                .orElse(-1);
    }
}
