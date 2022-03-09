package fi.fmi.avi.archiver.logging.model;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

import javax.annotation.Nullable;

import fi.fmi.avi.archiver.file.FileProcessingIdentifier;
import fi.fmi.avi.archiver.file.FileReference;
import fi.fmi.avi.archiver.message.MessagePositionInFile;

public final class SynchronizedLoggingContext implements LoggingContext {
    private final Object mutex = new Object();
    private final LoggingContext delegate;

    SynchronizedLoggingContext(final LoggingContext delegate) {
        this.delegate = requireNonNull(delegate, "delegate");
    }

    @Override
    public void appendTo(final StringBuilder builder) {
        synchronized (mutex) {
            delegate.appendTo(builder);
        }
    }

    @Override
    public int estimateLogStringLength() {
        synchronized (mutex) {
            return delegate.estimateLogStringLength();
        }
    }

    @Override
    public String toString() {
        synchronized (mutex) {
            return delegate.toString();
        }
    }

    @Override
    public ReadableLoggingContext readableCopy() {
        synchronized (mutex) {
            return delegate.readableCopy();
        }
    }

    @Override
    public String getStructureName() {
        synchronized (mutex) {
            return delegate.getStructureName();
        }
    }

    @Override
    public FileProcessingIdentifier getProcessingId() {
        synchronized (mutex) {
            return delegate.getProcessingId();
        }
    }

    @Override
    public Optional<FileReference> getFile() {
        synchronized (mutex) {
            return delegate.getFile();
        }
    }

    @Override
    public Optional<BulletinLogReference> getBulletin() {
        synchronized (mutex) {
            return delegate.getBulletin();
        }
    }

    @Override
    public int getBulletinIndex() {
        synchronized (mutex) {
            return delegate.getBulletinIndex();
        }
    }

    @Override
    public Optional<MessageLogReference> getMessage() {
        synchronized (mutex) {
            return delegate.getMessage();
        }
    }

    @Override
    public int getMessageIndex() {
        synchronized (mutex) {
            return delegate.getMessageIndex();
        }
    }

    @Override
    public void enterFile(@Nullable final FileReference file) {
        synchronized (mutex) {
            delegate.enterFile(file);
        }
    }

    @Override
    public void leaveFile() {
        synchronized (mutex) {
            delegate.leaveFile();
        }
    }

    @Override
    public void enterBulletin(@Nullable final BulletinLogReference bulletin) {
        synchronized (mutex) {
            delegate.enterBulletin(bulletin);
        }
    }

    @Override
    public void enterBulletin(final int index) {
        synchronized (mutex) {
            delegate.enterBulletin(index);
        }
    }

    @Override
    public void leaveBulletin() {
        synchronized (mutex) {
            delegate.leaveBulletin();
        }
    }

    @Override
    public List<BulletinLogReference> getAllBulletins() {
        synchronized (mutex) {
            return delegate.getAllBulletins();
        }
    }

    @Override
    public void modifyBulletin(final UnaryOperator<BulletinLogReference> operator) {
        synchronized (mutex) {
            delegate.modifyBulletin(operator);
        }
    }

    @Override
    public void enterMessage(@Nullable final MessageLogReference message) {
        synchronized (mutex) {
            delegate.enterMessage(message);
        }
    }

    @Override
    public void enterMessage(final int index) {
        synchronized (mutex) {
            delegate.enterMessage(index);
        }
    }

    @Override
    public void enterBulletinMessage(final MessagePositionInFile messagePositionInFile) {
        synchronized (mutex) {
            delegate.enterBulletinMessage(messagePositionInFile);
        }
    }

    @Override
    public void leaveMessage() {
        synchronized (mutex) {
            delegate.leaveMessage();
        }
    }

    @Override
    public List<MessageLogReference> getBulletinMessages() {
        synchronized (mutex) {
            return delegate.getBulletinMessages();
        }
    }

    @Override
    public void modifyMessage(final UnaryOperator<MessageLogReference> operator) {
        synchronized (mutex) {
            delegate.modifyMessage(operator);
        }
    }

    @Override
    public FileProcessingStatistics getStatistics() {
        synchronized (mutex) {
            return delegate.getStatistics();
        }
    }

    @Override
    public void initStatistics() {
        synchronized (mutex) {
            delegate.initStatistics();
        }
    }

    @Override
    public void recordProcessingResult(final FileProcessingStatistics.ProcessingResult processingResult) {
        synchronized (mutex) {
            delegate.recordProcessingResult(processingResult);
        }
    }
}
