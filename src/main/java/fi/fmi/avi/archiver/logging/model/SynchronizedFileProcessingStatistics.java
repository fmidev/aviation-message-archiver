package fi.fmi.avi.archiver.logging.model;

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
    public void initBulletins(final int amount) {
        synchronized (mutex) {
            delegate.initBulletins(amount);
        }
    }

    @Override
    public void initMessages(final int bulletinIndex, final int amount) {
        synchronized (mutex) {
            delegate.initMessages(bulletinIndex, amount);
        }
    }

    @Override
    public void recordMessageResult(final int bulletinIndex, final int messageIndex, final ProcessingResult processingResult) {
        synchronized (mutex) {
            delegate.recordMessageResult(bulletinIndex, messageIndex, processingResult);
        }
    }

    @Override
    public void recordBulletinResult(final int bulletinIndex, final ProcessingResult processingResult) {
        synchronized (mutex) {
            delegate.recordBulletinResult(bulletinIndex, processingResult);
        }
    }

    @Override
    public void recordFileResult(final ProcessingResult processingResult) {
        synchronized (mutex) {
            delegate.recordFileResult(processingResult);
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
    public ReadableFileProcessingStatistics readableCopy() {
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
    public ProcessingResult getFile() {
        synchronized (mutex) {
            return delegate.getFile();
        }
    }

    @Override
    public ResultStatistics getBulletin() {
        synchronized (mutex) {
            return delegate.getBulletin();
        }
    }

    @Override
    public int getBulletinTotal() {
        synchronized (mutex) {
            return delegate.getBulletinTotal();
        }
    }

    @Override
    public ResultStatistics getMessage() {
        synchronized (mutex) {
            return delegate.getMessage();
        }
    }

    @Override
    public int getMessageTotal() {
        synchronized (mutex) {
            return delegate.getBulletinTotal();
        }
    }
}
