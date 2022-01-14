package fi.fmi.avi.archiver.logging;

import static java.util.Objects.requireNonNull;

public final class SynchronizedFileProcessingStatistics implements FileProcessingStatistics {
    private final Object mutex = new Object();
    private final FileProcessingStatistics delegate;

    SynchronizedFileProcessingStatistics(final FileProcessingStatistics delegate) {
        this.delegate = requireNonNull(delegate, "delegate");
    }

    @Override
    public void appendTo(final StringBuilder builder) {
        synchronized (mutex) {
            delegate.appendTo(builder);
        }
    }

    @Override
    public void clear() {
        synchronized (mutex) {
            delegate.clear();
        }
    }

    @Override
    public void recordMessageStatus(final int bulletinIndex, final int messageIndex, final Status status) {
        synchronized (mutex) {
            delegate.recordMessageStatus(bulletinIndex, messageIndex, status);
        }
    }

    @Override
    public void recordBulletinStatus(final int bulletinIndex, final Status status) {
        synchronized (mutex) {
            delegate.recordBulletinStatus(bulletinIndex, status);
        }
    }

    @Override
    public void recordFileStatus(final Status status) {
        synchronized (mutex) {
            delegate.recordFileStatus(status);
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
}
