package fi.fmi.avi.archiver.logging;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

import javax.annotation.Nullable;

import fi.fmi.avi.archiver.file.FileReference;
import fi.fmi.avi.archiver.message.MessagePositionInFile;

public interface LoggingContext extends AppendingLoggable {
    @SuppressWarnings("ClassReferencesSubclass")
    static LoggingContext synchronize(final LoggingContext loggingContext) {
        return loggingContext instanceof SynchronizedLoggingContext ? loggingContext : new SynchronizedLoggingContext(loggingContext);
    }

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

    List<BulletinLogReference> getAllBulletinLogReferences();

    default void modifyBulletinLogReference(final UnaryOperator<BulletinLogReference> operator) {
        getBulletinLogReference()//
                .map(operator)//
                .ifPresent(this::enterBulletin);
    }

    default int getBulletinIndex() {
        return getBulletinLogReference().map(BulletinLogReference::getBulletinIndex).orElse(-1);
    }

    void enterMessage(@Nullable MessageLogReference messageLogReference);

    void enterMessage(int index);

    default void enterMessage(final MessagePositionInFile messagePositionInFile) {
        requireNonNull(messagePositionInFile, "messagePositionInFile");
        enterBulletin(messagePositionInFile.getBulletinIndex());
        enterMessage(messagePositionInFile.getMessageIndex());
    }

    default void leaveMessage() {
        enterMessage((MessageLogReference) null);
    }

    Optional<MessageLogReference> getMessageLogReference();

    List<MessageLogReference> getBulletinMessageLogReferences();

    default void modifyMessageLogReference(final UnaryOperator<MessageLogReference> operator) {
        getMessageLogReference()//
                .map(operator)//
                .ifPresent(this::enterMessage);
    }

    default int getMessageIndex() {
        return getMessageLogReference().map(MessageLogReference::getMessageIndex).orElse(-1);
    }

    FileProcessingStatistics getStatistics();

    void initStatistics();

    default void recordStatus(final FileProcessingStatistics.Status status) {
        requireNonNull(status, "status");
        final int bulletinIndex = getBulletinIndex();
        if (bulletinIndex < 0) {
            getStatistics().recordFileStatus(status);
        } else {
            final int messageIndex = getMessageIndex();
            if (messageIndex < 0) {
                getStatistics().recordBulletinStatus(bulletinIndex, status);
                leaveBulletin();
            } else {
                getStatistics().recordMessageStatus(bulletinIndex, messageIndex, status);
                leaveMessage();
            }
        }
    }
}
