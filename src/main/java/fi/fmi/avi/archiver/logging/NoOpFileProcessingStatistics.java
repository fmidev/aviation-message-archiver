package fi.fmi.avi.archiver.logging;

import static java.util.Objects.requireNonNull;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public final class NoOpFileProcessingStatistics extends AbstractNoOpLoggable implements FileProcessingStatistics {
    private static final NoOpFileProcessingStatistics INSTANCE = new NoOpFileProcessingStatistics();

    private NoOpFileProcessingStatistics() {
    }

    @SuppressFBWarnings(value = "MS_EXPOSE_REP", justification = "immutable value")
    public static NoOpFileProcessingStatistics getInstance() {
        return INSTANCE;
    }

    @Override
    public void clear() {
    }

    @Override
    public void recordMessageStatus(final int bulletinIndex, final int messageIndex, final Status status) {
        requireNonNull(status, "status");
    }

    @Override
    public void recordBulletinStatus(final int bulletinIndex, final Status status) {
        requireNonNull(status, "status");
    }

    @Override
    public void recordFileStatus(final Status status) {
        requireNonNull(status, "status");
    }

}
