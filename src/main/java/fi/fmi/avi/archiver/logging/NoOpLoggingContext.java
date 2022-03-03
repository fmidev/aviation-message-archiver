package fi.fmi.avi.archiver.logging;

import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

import javax.annotation.Nullable;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fi.fmi.avi.archiver.file.FileProcessingIdentifier;
import fi.fmi.avi.archiver.file.FileReference;
import fi.fmi.avi.archiver.message.MessagePositionInFile;

public final class NoOpLoggingContext extends AbstractNoOpLoggable implements LoggingContext {
    private static final NoOpLoggingContext INSTANCE = new NoOpLoggingContext();

    private NoOpLoggingContext() {
    }

    @SuppressFBWarnings(value = "MS_EXPOSE_REP", justification = "immutable value")
    public static NoOpLoggingContext getInstance() {
        return INSTANCE;
    }

    @Override
    public ReadableLoggingContext readableCopy() {
        return this;
    }

    @Override
    public FileProcessingIdentifier getProcessingId() {
        return FileProcessingIdentifier.newInstance();
    }

    @Override
    public Optional<FileReference> getFile() {
        return Optional.empty();
    }

    @Override
    public Optional<BulletinLogReference> getBulletin() {
        return Optional.empty();
    }

    @Override
    public int getBulletinIndex() {
        return -1;
    }

    @Override
    public Optional<MessageLogReference> getMessage() {
        return Optional.empty();
    }

    @Override
    public int getMessageIndex() {
        return -1;
    }

    @Override
    public void enterFile(@Nullable final FileReference file) {
    }

    @Override
    public void leaveFile() {
    }

    @Override
    public void enterBulletin(@Nullable final BulletinLogReference bulletin) {
    }

    @Override
    public void enterBulletin(final int index) {
    }

    @Override
    public void leaveBulletin() {
    }

    @Override
    public List<BulletinLogReference> getAllBulletins() {
        return Collections.emptyList();
    }

    @Override
    public void modifyBulletin(final UnaryOperator<BulletinLogReference> operator) {
        requireNonNull(operator, "operator");
    }

    @Override
    public void enterMessage(@Nullable final MessageLogReference message) {
    }

    @Override
    public void enterMessage(final int index) {
    }

    @Override
    public void enterBulletinMessage(final MessagePositionInFile messagePositionInFile) {
        requireNonNull(messagePositionInFile, "messagePositionInFile");
    }

    @Override
    public void leaveMessage() {
    }

    @Override
    public List<MessageLogReference> getBulletinMessages() {
        return Collections.emptyList();
    }

    @Override
    public void modifyMessage(final UnaryOperator<MessageLogReference> operator) {
        requireNonNull(operator, "operator");
    }

    @Override
    public FileProcessingStatistics getStatistics() {
        return NoOpFileProcessingStatistics.getInstance();
    }

    @Override
    public void initStatistics() {
    }

    @Override
    public void recordProcessingResult(final FileProcessingStatistics.ProcessingResult processingResult) {
        requireNonNull(processingResult, "processingResult");
    }
}
