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
    public void initBulletins(final int amount) {
    }

    @Override
    public void initMessages(final int bulletinIndex, final int amount) {
    }

    @Override
    public void recordMessageResult(final int bulletinIndex, final int messageIndex, final ProcessingResult processingResult) {
        requireNonNull(processingResult, "processingResult");
    }

    @Override
    public void recordBulletinResult(final int bulletinIndex, final ProcessingResult processingResult) {
        requireNonNull(processingResult, "processingResult");
    }

    @Override
    public void recordFileResult(final ProcessingResult processingResult) {
        requireNonNull(processingResult, "processingResult");
    }

}
