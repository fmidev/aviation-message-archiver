package fi.fmi.avi.archiver.logging;

import static java.util.Objects.requireNonNull;

import java.util.Iterator;

import com.google.auto.value.AutoValue;

@AutoValue
abstract class ImmutableFileProcessingStatistics extends AbstractFileProcessingStatistics {
    public static ImmutableFileProcessingStatistics create(final ProcessingResult file, final ResultStatistics bulletin, final ResultStatistics message) {
        return new AutoValue_ImmutableFileProcessingStatistics(requireNonNull(file, "file"),
                ImmutableResultStatistics.copyOf(requireNonNull(bulletin, "bulletin")), ImmutableResultStatistics.copyOf(requireNonNull(message, "message")));
    }

    public static ImmutableFileProcessingStatistics copyOf(final ReadableFileProcessingStatistics statistics) {
        return statistics instanceof ImmutableFileProcessingStatistics
                ? (ImmutableFileProcessingStatistics) statistics
                : create(statistics.getFile(), statistics.getBulletin(), statistics.getMessage());
    }

    @Override
    public ImmutableFileProcessingStatistics readableCopy() {
        return this;
    }

    @Override
    public abstract ImmutableResultStatistics getBulletin();

    @Override
    public abstract ImmutableResultStatistics getMessage();

    public static final class ImmutableResultStatistics extends AbstractResultStatistics {
        private static final int STATISTICS_LENGTH = ProcessingResult.getValues().size();
        private static final ImmutableResultStatistics EMPTY = new ImmutableResultStatistics(new int[STATISTICS_LENGTH]);

        private final int[] statistics;

        private ImmutableResultStatistics(final int[] statistics) {
            this.statistics = statistics;
        }

        public static ImmutableResultStatistics compute(final Iterator<ProcessingResult> processingResults) {
            requireNonNull(processingResults, "processingResults");
            final int[] statistics = new int[STATISTICS_LENGTH];
            while (processingResults.hasNext()) {
                statistics[processingResults.next().ordinal()]++;
            }
            return new ImmutableResultStatistics(statistics);
        }

        public static ImmutableResultStatistics copyOf(final ResultStatistics resultStatistics) {
            requireNonNull(resultStatistics, "resultStatistics");
            if (resultStatistics instanceof ImmutableResultStatistics) {
                return (ImmutableResultStatistics) resultStatistics;
            }
            final int[] statistics = new int[STATISTICS_LENGTH];
            for (final ProcessingResult processingResult : ProcessingResult.getValues()) {
                statistics[processingResult.ordinal()] = resultStatistics.get(processingResult);
            }
            return new ImmutableResultStatistics(statistics);
        }

        public static ImmutableResultStatistics empty() {
            return EMPTY;
        }

        @Override
        public int get(final ProcessingResult processingResult) {
            return statistics[processingResult.ordinal()];
        }

        @Override
        public ImmutableResultStatistics readableCopy() {
            return this;
        }
    }
}
