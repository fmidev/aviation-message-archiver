package fi.fmi.avi.archiver.logging;

import java.util.Optional;
import java.util.function.UnaryOperator;

import javax.annotation.Nullable;

import fi.fmi.avi.archiver.file.FileReference;
import fi.fmi.avi.archiver.message.MessageReference;

public interface LoggingContext extends AppendingLoggable {
    void enterFile(@Nullable FileReference fileReference);

    default void leaveFile() {
        enterFile(null);
    }

    Optional<FileReference> getFileReference();

    void enterBulletin(@Nullable BulletinLogReference bulletinLogReference);

    void enterBulletin(int index);

    default void leaveBulletin() {
        enterBulletin(null);
    }

    Optional<BulletinLogReference> getBulletinLogReference();

    default void modifyBulletinReference(final UnaryOperator<BulletinLogReference> operator) {
        getBulletinLogReference()//
                .map(operator)//
                .ifPresent(this::enterBulletin);
    }

    default int getBulletinIndex() {
        return getBulletinLogReference().map(BulletinLogReference::getBulletinIndex).orElse(-1);
    }

    void enterMessage(@Nullable MessageLogReference messageLogReference);

    void enterMessage(MessageReference messageReference);

    default void leaveMessage() {
        enterMessage((MessageLogReference) null);
    }

    Optional<MessageLogReference> getMessageLogReference();

    default void modifyMessageReference(final UnaryOperator<MessageLogReference> operator) {
        getMessageLogReference()//
                .map(operator)//
                .ifPresent(this::enterMessage);
    }

    default int getMessageIndex() {
        return getMessageLogReference().map(MessageLogReference::getMessageIndex).orElse(-1);
    }
}
