package fi.fmi.avi.archiver.message;

import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;

/**
 * This class represents message index position within a file in terms of bulletin index within file and message index within bulletin.
 */
@AutoValue
public abstract class MessagePositionInFile {
    MessagePositionInFile() {
    }

    /**
     * Return new instance pointing at provided indices.
     *
     * @param bulletinIndex
     *         bulletin index within file, starting at zero
     * @param messageIndex
     *         message index within bulletin, starting at zero
     *
     * @return new instance pointing at provided indices
     */
    public static MessagePositionInFile getInstance(final int bulletinIndex, final int messageIndex) {
        Preconditions.checkArgument(bulletinIndex >= 0, "bulletinIndex must be non-negative");
        Preconditions.checkArgument(messageIndex >= 0, "messageIndex must be non-negative");
        return bulletinIndex == 0 && messageIndex == 0 ? getInitial() : new AutoValue_MessagePositionInFile(bulletinIndex, messageIndex);
    }

    /**
     * Return initial instance of bulletin and message index at zero.
     *
     * @return initial instance of bulletin and message index at zero
     */
    public static MessagePositionInFile getInitial() {
        return InitialValueHolder.INITIAL_VALUE;
    }

    /**
     * Return bulletin index within file.
     *
     * @return bulletin index within file
     */
    public abstract int getBulletinIndex();

    /**
     * Return message index within bulletin.
     *
     * @return message index within bulletin
     */
    public abstract int getMessageIndex();

    @Override
    public String toString() {
        return getBulletinIndex() + "." + getMessageIndex();
    }

    private static final class InitialValueHolder {
        static final MessagePositionInFile INITIAL_VALUE = new AutoValue_MessagePositionInFile(0, 0);
    }
}
