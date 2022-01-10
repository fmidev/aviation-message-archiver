package fi.fmi.avi.archiver.logging;

import static java.util.Objects.requireNonNull;

import java.util.Optional;
import java.util.function.UnaryOperator;

import javax.annotation.Nullable;

import fi.fmi.avi.archiver.file.FileReference;
import fi.fmi.avi.archiver.message.MessageReference;

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
    public String toString() {
        synchronized (mutex) {
            return delegate.toString();
        }
    }

    @Override
    public void enterFile(@Nullable final FileReference fileReference) {
        synchronized (mutex) {
            delegate.enterFile(fileReference);
        }
    }

    @Override
    public void leaveFile() {
        synchronized (mutex) {
            delegate.leaveFile();
        }
    }

    @Override
    public Optional<FileReference> getFileReference() {
        synchronized (mutex) {
            return delegate.getFileReference();
        }
    }

    @Override
    public void enterBulletin(@Nullable final BulletinLogReference bulletinLogReference) {
        synchronized (mutex) {
            delegate.enterBulletin(bulletinLogReference);
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
    public Optional<BulletinLogReference> getBulletinLogReference() {
        synchronized (mutex) {
            return delegate.getBulletinLogReference();
        }
    }

    @Override
    public void modifyBulletinReference(final UnaryOperator<BulletinLogReference> operator) {
        synchronized (mutex) {
            delegate.modifyBulletinReference(operator);
        }
    }

    @Override
    public int getBulletinIndex() {
        synchronized (mutex) {
            return delegate.getBulletinIndex();
        }
    }

    @Override
    public void enterMessage(@Nullable final MessageLogReference messageLogReference) {
        synchronized (mutex) {
            delegate.enterMessage(messageLogReference);
        }
    }

    @Override
    public void enterMessage(final MessageReference messageReference) {
        synchronized (mutex) {
            delegate.enterMessage(messageReference);
        }
    }

    @Override
    public void leaveMessage() {
        synchronized (mutex) {
            delegate.leaveMessage();
        }
    }

    @Override
    public Optional<MessageLogReference> getMessageLogReference() {
        synchronized (mutex) {
            return delegate.getMessageLogReference();
        }
    }

    @Override
    public void modifyMessageReference(final UnaryOperator<MessageLogReference> operator) {
        synchronized (mutex) {
            delegate.modifyMessageReference(operator);
        }
    }

    @Override
    public int getMessageIndex() {
        synchronized (mutex) {
            return delegate.getMessageIndex();
        }
    }

    @Override
    public FileProcessingStatistics getStatistics() {
        synchronized (mutex) {
            return delegate.getStatistics();
        }
    }

    @Override
    public void recordStatus(final FileProcessingStatistics.Status status) {
        synchronized (mutex) {
            delegate.recordStatus(status);
        }
    }
}
