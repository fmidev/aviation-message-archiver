package fi.fmi.avi.archiver.message;

import java.util.Objects;

import com.google.common.base.Preconditions;

public final class MessageReference {
    private static final MessageReference INITIAL = getInstance(0, 0);
    private final int bulletinIndex;
    private final int messageIndex;

    private MessageReference(final int bulletinIndex, final int messageIndex) {
        Preconditions.checkState(bulletinIndex >= 0, "bulletinIndex must be non-negative");
        Preconditions.checkState(messageIndex >= 0, "messageIndex must be non-negative");
        this.bulletinIndex = bulletinIndex;
        this.messageIndex = messageIndex;
    }

    public static MessageReference getInstance(final int bulletinIndex, final int messageIndex) {
        return new MessageReference(bulletinIndex, messageIndex);
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
        return Objects.hash(bulletinIndex, messageIndex);
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof MessageReference) {
            final MessageReference other = (MessageReference) obj;
            return this.bulletinIndex == other.bulletinIndex //
                    && this.messageIndex == other.messageIndex;
        } else {
            return false;
        }
    }
}
