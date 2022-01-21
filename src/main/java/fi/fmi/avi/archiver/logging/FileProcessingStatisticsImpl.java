package fi.fmi.avi.archiver.logging;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Stream;

public final class FileProcessingStatisticsImpl extends AbstractAppendingLoggable implements FileProcessingStatistics {
    private static final int FILE_STATISTICS_LENGTH_ESTIMATE = 4;
    private static final int STATISTICS_CATEGORIES_COUNT = 2; // message, bulletin
    private static final int STRING_LENGTH_ESTIMATE = (Statistics.STRING_LENGTH_ESTIMATE + 4) * STATISTICS_CATEGORIES_COUNT + FILE_STATISTICS_LENGTH_ESTIMATE;

    private final ArrayList<ArrayList<ProcessingResult>> bulletinMessageProcessingResults = new ArrayList<>(0);
    private final ArrayList<ProcessingResult> bulletinProcessingResults = new ArrayList<>(0);

    private ProcessingResult fileProcessingResult = INITIAL_PROCESSING_RESULT;

    private static void ensureProcessingResultsSizeAtLeast(final ArrayList<ProcessingResult> processingResults, final int minSize) {
        ensureSizeAtLeast(processingResults, minSize, i -> INITIAL_PROCESSING_RESULT);
    }

    private static <E> void ensureSizeAtLeast(final ArrayList<E> list, final int minSize, final IntFunction<E> defaultElement) {
        list.ensureCapacity(minSize);
        for (int nextIndex = list.size(); nextIndex < minSize; nextIndex++) {
            list.add(defaultElement.apply(nextIndex));
        }
    }

    private static <E> Optional<E> getElement(final List<E> list, final int index) {
        return list.size() > index ? Optional.of(list.get(index)) : Optional.empty();
    }

    private static ProcessingResult max(final ProcessingResult processingResult1, final ProcessingResult processingResult2) {
        return ProcessingResult.getComparator().compare(processingResult1, processingResult2) >= 0 ? processingResult1 : processingResult2;
    }

    @Override
    public void clear() {
        bulletinMessageProcessingResults.clear();
        bulletinMessageProcessingResults.trimToSize();
        bulletinProcessingResults.clear();
        bulletinProcessingResults.trimToSize();
        fileProcessingResult = INITIAL_PROCESSING_RESULT;
    }

    @Override
    public void initBulletins(final int amount) {
        if (amount > 0) {
            ensureProcessingResultsSizeAtLeast(bulletinProcessingResults, amount);
        }
    }

    @Override
    public void initMessages(final int bulletinIndex, final int amount) {
        if (bulletinIndex >= 0 && amount > 0) {
            ensureProcessingResultsSizeAtLeast(getBulletinMessageProcessingResults(bulletinIndex), amount);
        }
    }

    @Override
    public void recordMessageResult(final int bulletinIndex, final int messageIndex, final ProcessingResult processingResult) {
        requireNonNull(processingResult, "processingResult");
        recordProcessingResult(getBulletinMessageProcessingResults(bulletinIndex), messageIndex, processingResult);
    }

    private ArrayList<ProcessingResult> getBulletinMessageProcessingResults(final int bulletinIndex) {
        ensureSizeAtLeast(bulletinMessageProcessingResults, bulletinIndex + 1, i -> new ArrayList<>(0));
        return bulletinMessageProcessingResults.get(bulletinIndex);
    }

    private void recordProcessingResult(final ArrayList<ProcessingResult> processingResults, final int index, final ProcessingResult processingResult) {
        ensureProcessingResultsSizeAtLeast(processingResults, index + 1);
        final ProcessingResult oldProcessingResult = processingResults.get(index);
        processingResults.set(index, max(oldProcessingResult, processingResult));
    }

    @Override
    public void recordBulletinResult(final int bulletinIndex, final ProcessingResult processingResult) {
        requireNonNull(processingResult, "processingResult");
        recordProcessingResult(bulletinProcessingResults, bulletinIndex, processingResult);
    }

    private ProcessingResult getFileProcessingResult() {
        return Stream.of(//
                        bulletinMessageProcessingResults.stream()//
                                .flatMap(Collection::stream), //
                        bulletinProcessingResults.stream(), //
                        Stream.of(this.fileProcessingResult))//
                .flatMap(Function.identity())//
                .max(ProcessingResult.getComparator())//
                .orElse(INITIAL_PROCESSING_RESULT);
    }

    @Override
    public void recordFileResult(final ProcessingResult processingResult) {
        requireNonNull(processingResult, "processingResult");
        fileProcessingResult = max(fileProcessingResult, processingResult);
    }

    private Statistics getBulletinStatistics() {
        final Statistics bulletinStatistics = new Statistics();
        for (int i = 0, size = Math.max(bulletinMessageProcessingResults.size(), bulletinProcessingResults.size()); i < size; i++) {
            bulletinStatistics.add(getBulletinProcessingResult(i));
        }
        return bulletinStatistics;
    }

    private ProcessingResult getBulletinProcessingResult(final int index) {
        final ProcessingResult bulletinMessageProcessingResult = getElement(bulletinMessageProcessingResults, index)//
                .flatMap(processingResults -> processingResults.stream()//
                        .max(ProcessingResult.getComparator()))//
                .orElse(INITIAL_PROCESSING_RESULT);
        final ProcessingResult bulletinProcessingResult = getElement(bulletinProcessingResults, index)//
                .orElse(INITIAL_PROCESSING_RESULT);
        return max(bulletinMessageProcessingResult, bulletinProcessingResult);
    }

    private Statistics getMessageStatistics() {
        final Statistics messageStatistics = new Statistics();
        bulletinMessageProcessingResults.forEach(processingResults -> processingResults.forEach(messageStatistics::add));
        return messageStatistics;
    }

    @Override
    public void appendTo(final StringBuilder builder) {
        builder.append("M{");
        getMessageStatistics().appendTo(builder);
        builder.append("} B{");
        getBulletinStatistics().appendTo(builder);
        builder.append("} F{")//
                .append(getFileProcessingResult().getAbbreviatedName())//
                .append("}");
    }

    @Override
    public int estimateLogStringLength() {
        return STRING_LENGTH_ESTIMATE;
    }

    private static final class Statistics extends AbstractAppendingLoggable {
        private static final int STRING_LENGTH_PER_PROCESSING_RESULT_ESTIMATE = 3 /* control chars */ + 4 /* number */;
        static final int STRING_LENGTH_ESTIMATE = (ProcessingResult.getValues().size() + 1) * STRING_LENGTH_PER_PROCESSING_RESULT_ESTIMATE;

        private final int[] statistics = new int[ProcessingResult.getValues().size()];

        private Statistics() {
        }

        public void add(final ProcessingResult processingResult) {
            statistics[processingResult.ordinal()]++;
        }

        @Override
        public void appendTo(final StringBuilder builder) {
            int total = 0;
            for (final ProcessingResult processingResult : ProcessingResult.getValues()) {
                final int amount = statistics[processingResult.ordinal()];
                total += amount;
                if (amount > 0) {
                    builder.append(processingResult.getAbbreviatedName())//
                            .append(':')//
                            .append(amount)//
                            .append(',');
                }
            }
            builder//
                    .append("T:")//
                    .append(total);
        }

        @Override
        public int estimateLogStringLength() {
            return STRING_LENGTH_ESTIMATE;
        }
    }
}
