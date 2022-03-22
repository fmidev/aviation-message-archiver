package fi.fmi.avi.archiver.message;

import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;

@AutoValue
public abstract class MessagePositionInFile {
    MessagePositionInFile() {
    }

    public static MessagePositionInFile getInstance(final int bulletinIndex, final int messageIndex) {
        Preconditions.checkArgument(bulletinIndex >= 0, "bulletinIndex must be non-negative");
        Preconditions.checkArgument(messageIndex >= 0, "messageIndex must be non-negative");
        return bulletinIndex == 0 && messageIndex == 0 ? getInitial() : new AutoValue_MessagePositionInFile(bulletinIndex, messageIndex);
    }

    public static MessagePositionInFile getInitial() {
        return InitialValueHolder.INITIAL_VALUE;
    }

    public abstract int getBulletinIndex();

    public abstract int getMessageIndex();

    @Override
    public String toString() {
        return getBulletinIndex() + "." + getMessageIndex();
    }

    private static final class InitialValueHolder {
        static final MessagePositionInFile INITIAL_VALUE = new AutoValue_MessagePositionInFile(0, 0);
    }
}
