package fi.fmi.avi.archiver.logging;

import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

import javax.annotation.Nullable;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fi.fmi.avi.archiver.file.FileReference;
import fi.fmi.avi.archiver.message.MessageReference;

public final class NoOpLoggingContext extends AbstractNoOpLoggable implements LoggingContext {
    private static final NoOpLoggingContext INSTANCE = new NoOpLoggingContext();

    private NoOpLoggingContext() {
    }

    @SuppressFBWarnings(value = "MS_EXPOSE_REP", justification = "immutable value")
    public static NoOpLoggingContext getInstance() {
        return INSTANCE;
    }

    @Override
    public void enterFile(@Nullable final FileReference fileReference) {
    }

    @Override
    public void leaveFile() {
    }

    @Override
    public Optional<FileReference> getFileReference() {
        return Optional.empty();
    }

    @Override
    public void enterBulletin(@Nullable final BulletinLogReference bulletinLogReference) {
    }

    @Override
    public void enterBulletin(final int index) {
    }

    @Override
    public void leaveBulletin() {
    }

    @Override
    public Optional<BulletinLogReference> getBulletinLogReference() {
        return Optional.empty();
    }

    @Override
    public List<BulletinLogReference> getAllBulletinLogReferences() {
        return Collections.emptyList();
    }

    @Override
    public void modifyBulletinLogReference(final UnaryOperator<BulletinLogReference> operator) {
        requireNonNull(operator, "operator");
    }

    @Override
    public int getBulletinIndex() {
        return -1;
    }

    @Override
    public void enterMessage(@Nullable final MessageLogReference messageLogReference) {
    }

    @Override
    public void enterMessage(final int index) {
    }

    @Override
    public void enterMessage(final MessageReference messageReference) {
        requireNonNull(messageReference, "messageReference");
    }

    @Override
    public void leaveMessage() {
    }

    @Override
    public Optional<MessageLogReference> getMessageLogReference() {
        return Optional.empty();
    }

    @Override
    public List<MessageLogReference> getBulletinMessageLogReferences() {
        return Collections.emptyList();
    }

    @Override
    public void modifyMessageLogReference(final UnaryOperator<MessageLogReference> operator) {
        requireNonNull(operator, "operator");
    }

    @Override
    public int getMessageIndex() {
        return -1;
    }

    @Override
    public FileProcessingStatistics getStatistics() {
        return NoOpFileProcessingStatistics.getInstance();
    }

    @Override
    public void initStatistics() {
    }

    @Override
    public void recordStatus(final FileProcessingStatistics.Status status) {
        requireNonNull(status, "status");
    }
}
