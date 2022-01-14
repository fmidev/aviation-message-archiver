package fi.fmi.avi.archiver.message;

import com.google.common.base.Preconditions;

public final class MessageReference {
    private static final MessageReference INITIAL = new MessageReference(0, 0);

    private final int bulletinIndex;
    private final int messageIndex;

    private MessageReference(final int bulletinIndex, final int messageIndex) {
        Preconditions.checkArgument(bulletinIndex >= 0, "bulletinIndex must be non-negative");
        Preconditions.checkArgument(messageIndex >= 0, "messageIndex must be non-negative");
        this.bulletinIndex = bulletinIndex;
        this.messageIndex = messageIndex;
    }

    public static MessageReference getInstance(final int bulletinIndex, final int messageIndex) {
        return bulletinIndex == 0 && messageIndex == 0 ? INITIAL : new MessageReference(bulletinIndex, messageIndex);
    }

    public static MessageReference getInitial() {
        return INITIAL;
    }

    public int getBulletinIndex() {
        return bulletinIndex;
    }

    public int getMessageIndex() {
        return messageIndex;
    }

    @Override
    public String toString() {
        return bulletinIndex + "." + messageIndex;
    }

    @Override
    public int hashCode() {
        return 997 * bulletinIndex + messageIndex;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof MessageReference) {
            final MessageReference other = (MessageReference) obj;
            return this.bulletinIndex == other.bulletinIndex //
                    && this.messageIndex == other.messageIndex;
        } else {
            return false;
        }
    }
}
