package fi.fmi.avi.archiver.logging;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import fi.fmi.avi.archiver.logging.ImmutableFileProcessingStatistics.ImmutableResultStatistics;

public final class FileProcessingStatisticsImpl extends AbstractFileProcessingStatistics implements FileProcessingStatistics {
    private final ArrayList<ArrayList<ProcessingResult>> recordedBulletinMessageResults = new ArrayList<>(0);
    private final ArrayList<ProcessingResult> recordedBulletinResults = new ArrayList<>(0);

    private ProcessingResult recordedFileResult = INITIAL_PROCESSING_RESULT;

    private ProcessingResult computedFileStatistics = INITIAL_PROCESSING_RESULT;
    private ResultStatistics computedBulletinStatistics = ImmutableResultStatistics.empty();
    private ResultStatistics computedMessageStatistics = ImmutableResultStatistics.empty();
    private int currentRevision;
    private int computedFileStatisticsRevision;
    private int computedBulletinStatisticsRevision;
    private int computedMessageStatisticsRevision;

    private static <E> Optional<E> getElement(final List<E> list, final int index) {
        return list.size() > index ? Optional.of(list.get(index)) : Optional.empty();
    }

    private static ProcessingResult max(final ProcessingResult processingResult1, final ProcessingResult processingResult2) {
        return ProcessingResult.getComparator().compare(processingResult1, processingResult2) >= 0 ? processingResult1 : processingResult2;
    }

    private void ensureProcessingResultsSizeAtLeast(final ArrayList<ProcessingResult> processingResults, final int minSize) {
        ensureSizeAtLeast(processingResults, minSize, i -> INITIAL_PROCESSING_RESULT);
    }

    private <E> void ensureSizeAtLeast(final ArrayList<E> list, final int minSize, final IntFunction<E> defaultElement) {
        final int listSize = list.size();
        if (listSize >= minSize) {
            return;
        }
        currentRevision++;
        list.ensureCapacity(minSize);
        for (int nextIndex = listSize; nextIndex < minSize; nextIndex++) {
            list.add(defaultElement.apply(nextIndex));
        }
    }

    @Override
    public void clear() {
        currentRevision = 0;
        recordedBulletinMessageResults.clear();
        recordedBulletinMessageResults.trimToSize();
        computedMessageStatistics = ImmutableResultStatistics.empty();
        recordedBulletinResults.clear();
        recordedBulletinResults.trimToSize();
        computedBulletinStatistics = ImmutableResultStatistics.empty();
        recordedFileResult = INITIAL_PROCESSING_RESULT;
    }

    @Override
    public void initBulletins(final int amount) {
        if (amount > 0) {
            ensureProcessingResultsSizeAtLeast(recordedBulletinResults, amount);
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
        ensureSizeAtLeast(recordedBulletinMessageResults, bulletinIndex + 1, i -> new ArrayList<>(0));
        return recordedBulletinMessageResults.get(bulletinIndex);
    }

    private void recordProcessingResult(final ArrayList<ProcessingResult> processingResults, final int index, final ProcessingResult processingResult) {
        ensureProcessingResultsSizeAtLeast(processingResults, index + 1);
        final ProcessingResult oldProcessingResult = processingResults.get(index);
        final ProcessingResult newProcessingResult = max(oldProcessingResult, processingResult);
        if (newProcessingResult != oldProcessingResult) {
            currentRevision++;
            processingResults.set(index, newProcessingResult);
        }
    }

    @Override
    public void recordBulletinResult(final int bulletinIndex, final ProcessingResult processingResult) {
        requireNonNull(processingResult, "processingResult");
        recordProcessingResult(recordedBulletinResults, bulletinIndex, processingResult);
    }

    @Override
    public void recordFileResult(final ProcessingResult processingResult) {
        requireNonNull(processingResult, "processingResult");
        final ProcessingResult newFileResult = max(recordedFileResult, processingResult);
        if (newFileResult != recordedFileResult) {
            currentRevision++;
            recordedFileResult = newFileResult;
        }
    }

    @Override
    public ProcessingResult getFile() {
        if (computedFileStatisticsRevision != currentRevision) {
            computedFileStatistics = Stream.of(//
                            recordedBulletinMessageResults.stream()//
                                    .flatMap(Collection::stream), //
                            recordedBulletinResults.stream(), //
                            Stream.of(this.recordedFileResult))//
                    .flatMap(Function.identity())//
                    .max(ProcessingResult.getComparator())//
                    .orElse(INITIAL_PROCESSING_RESULT);
            computedFileStatisticsRevision = currentRevision;
        }
        return computedFileStatistics;
    }

    @Override
    public ResultStatistics getBulletin() {
        if (computedBulletinStatisticsRevision != currentRevision) {
            computedBulletinStatistics = ImmutableResultStatistics.compute(
                    IntStream.range(0, Math.max(recordedBulletinMessageResults.size(), recordedBulletinResults.size()))//
                            .mapToObj(this::getBulletinProcessingResult)//
                            .iterator());
            computedBulletinStatisticsRevision = currentRevision;
        }
        return computedBulletinStatistics;
    }

    private ProcessingResult getBulletinProcessingResult(final int index) {
        final ProcessingResult bulletinMessageProcessingResult = getElement(recordedBulletinMessageResults, index)//
                .flatMap(processingResults -> processingResults.stream()//
                        .max(ProcessingResult.getComparator()))//
                .orElse(INITIAL_PROCESSING_RESULT);
        final ProcessingResult bulletinProcessingResult = getElement(recordedBulletinResults, index)//
                .orElse(INITIAL_PROCESSING_RESULT);
        return max(bulletinMessageProcessingResult, bulletinProcessingResult);
    }

    @Override
    public ResultStatistics getMessage() {
        if (computedMessageStatisticsRevision != currentRevision) {
            computedMessageStatistics = ImmutableResultStatistics.compute(recordedBulletinMessageResults.stream()//
                    .flatMap(Collection::stream)//
                    .iterator());
            computedMessageStatisticsRevision = currentRevision;
        }
        return computedMessageStatistics;
    }
}
