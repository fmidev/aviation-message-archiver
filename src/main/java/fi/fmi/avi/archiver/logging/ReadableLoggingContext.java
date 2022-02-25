package fi.fmi.avi.archiver.logging;

import java.util.Optional;

import fi.fmi.avi.archiver.file.FileProcessingIdentifier;
import fi.fmi.avi.archiver.file.FileReference;

public interface ReadableLoggingContext extends AppendingLoggable, StructuredLoggable {
    /**
     * {@inheritDoc}
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
     * The default implementation returns always {@code "loggingContext"}.
     * </p>
     *
     * @return {@inheritDoc}
     */
    @Override
    default String getStructureName() {
        return "processingContext";
    }

    /**
     * Returns file processing identifier.
     *
     * @return file processing identifier
     */
    FileProcessingIdentifier getFileProcessingIdentifier();

    /**
     * Return {@code FileReference} referring to the file currently under processing, if one is registered, otherwise empty.
     *
     * @return {@code FileReference} referring to the file currently under processing, if one is registered, otherwise empty
     */
    Optional<FileReference> getFileReference();

    /**
     * Return a {@code BulletinLogReference} referring to the bulletin currently under processing, if one is registered, otherwise empty.
     *
     * @return a {@code BulletinLogReference} referring to the bulletin currently under processing, if one is registered, otherwise empty.
     */
    Optional<BulletinLogReference> getBulletinLogReference();

    /**
     * Return the index of the currently processed bulletin within a file starting from index {@code 0}. Returns {@code -1} if no bulletin is currently
     * being processed.
     *
     * <p>
     * The default implementation returns the bulletin index of {@code BulletinLogReference} returned by {@link #getBulletinLogReference()}.
     * </p>
     *
     * @return index of bulletin currently under processing, or {@code -1}
     */
    default int getBulletinIndex() {
        return getBulletinLogReference()//
                .map(BulletinLogReference::getBulletinIndex)//
                .orElse(-1);
    }

    /**
     * Return a {@code MessageLogReference} referring to the message being currently processed, if one is registered, otherwise empty.
     *
     * @return {@code MessageLogReference} referring to message being currently under processing, if one is registered, otherwise empty
     */
    Optional<MessageLogReference> getMessageLogReference();

    /**
     * Return the index of the currently processed message within a bulletin starting from index {@code 0}. Returns {@code -1} if no message is currently being
     * processed.
     *
     * <p>
     * The default implementation returns the message index of {@code MessageLogReference} returned by {@link #getMessageLogReference()}.
     * </p>
     *
     * @return index of message being currently under processing, or {@code -1}
     */
    default int getMessageIndex() {
        return getMessageLogReference()//
                .map(MessageLogReference::getMessageIndex)//
                .orElse(-1);
    }
}
