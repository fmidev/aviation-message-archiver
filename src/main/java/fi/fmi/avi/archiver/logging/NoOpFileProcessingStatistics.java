package fi.fmi.avi.archiver.logging;

import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.Map;

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

    @Override
    public ReadableFileProcessingStatistics readableCopy() {
        return this;
    }

    @Override
    public ProcessingResult getFile() {
        return ProcessingResult.NOTHING;
    }

    @Override
    public ResultStatistics getBulletin() {
        return NoOpResultStatistics.getInstance();
    }

    @Override
    public ResultStatistics getMessage() {
        return NoOpResultStatistics.getInstance();
    }

    private static class NoOpResultStatistics extends AbstractNoOpLoggable implements ResultStatistics {
        private static final NoOpResultStatistics INSTANCE = new NoOpResultStatistics();

        public static NoOpResultStatistics getInstance() {
            return INSTANCE;
        }

        @Override
        public int get(final ProcessingResult processingResult) {
            return 0;
        }

        @Override
        public int getTotal() {
            return 0;
        }

        @Override
        public Map<ProcessingResult, Integer> asMap() {
            return Collections.emptyMap();
        }

        @Override
        public ResultStatistics readableCopy() {
            return this;
        }
    }
}
