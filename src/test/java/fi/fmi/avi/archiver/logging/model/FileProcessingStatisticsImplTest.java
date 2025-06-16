package fi.fmi.avi.archiver.logging.model;

import static fi.fmi.avi.archiver.logging.LoggableTests.assertDecentLengthEstimate;
import static fi.fmi.avi.archiver.logging.model.ReadableFileProcessingStatistics.ProcessingResult.ARCHIVED;
import static fi.fmi.avi.archiver.logging.model.ReadableFileProcessingStatistics.ProcessingResult.DISCARDED;
import static fi.fmi.avi.archiver.logging.model.ReadableFileProcessingStatistics.ProcessingResult.FAILED;
import static fi.fmi.avi.archiver.logging.model.ReadableFileProcessingStatistics.ProcessingResult.NOTHING;
import static fi.fmi.avi.archiver.logging.model.ReadableFileProcessingStatistics.ProcessingResult.REJECTED;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import javax.annotation.Nullable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fi.fmi.avi.archiver.logging.StructuredLoggable;
import fi.fmi.avi.archiver.logging.model.ReadableFileProcessingStatistics.ProcessingResult;
import fi.fmi.avi.archiver.logging.model.ReadableFileProcessingStatistics.ResultStatistics;

class FileProcessingStatisticsImplTest {
    private FileProcessingStatisticsImpl statistics;

    private static <T, E> T assertSameInstanceOnSubsequentInvocations(final Supplier<T> resultStatisticsSupplier, final Function<T, E> toEquatable) {
        return assertSameInstanceOnSubsequentInvocationsAfterChange(null, resultStatisticsSupplier, toEquatable);
    }

    private static <T, E> T assertSameInstanceOnSubsequentInvocationsAfterChange(@Nullable final T oldResultStatistics,
            final Supplier<T> resultStatisticsSupplier, final Function<T, E> toEquatable) {
        final T newResultStatistics = resultStatisticsSupplier.get();
        if (oldResultStatistics != null) {
            assertThat(toEquatable.apply(newResultStatistics)).isNotEqualTo(toEquatable.apply(oldResultStatistics));
        }
        assertThat(resultStatisticsSupplier.get()).as("repeated invocation 1").isSameAs(newResultStatistics);
        assertThat(resultStatisticsSupplier.get()).as("repeated invocation 2").isSameAs(newResultStatistics);
        return newResultStatistics;
    }

    @BeforeEach
    void setUp() {
        statistics = new FileProcessingStatisticsImpl();
    }

    @Test
    void getStructureName_returns_default_name_for_FileProcessingStatistics() {
        assertThat(statistics.getStructureName()).isEqualTo(StructuredLoggable.defaultStructureName(FileProcessingStatistics.class));
    }

    @Test
    void readableCopy_returns_ImmutableFileProcessingStatistics() {
        final ReadableFileProcessingStatistics readableCopy = statistics.readableCopy();
        assertThat(readableCopy).isInstanceOf(ImmutableFileProcessingStatistics.class);
        FileProcessingStatisticsSpec.from(statistics).assertEquals(readableCopy);
    }

    @Test
    void getFile_returns_same_instance_on_subsequent_invocations_between_changes() {
        ProcessingResult resultStatistics;
        final Supplier<ProcessingResult> resultStatisticsSupplier = () -> statistics.getFile();
        final Function<ProcessingResult, ProcessingResult> toEquatable = Function.identity();

        assertSameInstanceOnSubsequentInvocations(resultStatisticsSupplier, toEquatable);
        statistics.initMessages(2, 3);
        resultStatistics = assertSameInstanceOnSubsequentInvocations(resultStatisticsSupplier, toEquatable);
        statistics.recordMessageResult(4, 5, ARCHIVED);
        resultStatistics = assertSameInstanceOnSubsequentInvocationsAfterChange(resultStatistics, resultStatisticsSupplier, toEquatable);
        statistics.recordBulletinResult(6, DISCARDED);
        resultStatistics = assertSameInstanceOnSubsequentInvocationsAfterChange(resultStatistics, resultStatisticsSupplier, toEquatable);
        statistics.recordFileResult(REJECTED);
        resultStatistics = assertSameInstanceOnSubsequentInvocationsAfterChange(resultStatistics, resultStatisticsSupplier, toEquatable);
        statistics.clear();
        assertSameInstanceOnSubsequentInvocationsAfterChange(resultStatistics, resultStatisticsSupplier, toEquatable);
    }

    @Test
    void getBulletin_returns_same_instance_on_subsequent_invocations_between_changes() {
        ResultStatistics resultStatistics;
        final Supplier<ResultStatistics> resultStatisticsSupplier = () -> statistics.getBulletin();
        final Function<ResultStatistics, Map<ProcessingResult, Integer>> toEquatable = ResultStatistics::asMap;

        resultStatistics = assertSameInstanceOnSubsequentInvocations(resultStatisticsSupplier, toEquatable);
        statistics.initMessages(2, 3);
        resultStatistics = assertSameInstanceOnSubsequentInvocationsAfterChange(resultStatistics, resultStatisticsSupplier, toEquatable);
        statistics.recordMessageResult(4, 5, ARCHIVED);
        resultStatistics = assertSameInstanceOnSubsequentInvocationsAfterChange(resultStatistics, resultStatisticsSupplier, toEquatable);
        statistics.recordBulletinResult(6, DISCARDED);
        assertSameInstanceOnSubsequentInvocationsAfterChange(resultStatistics, resultStatisticsSupplier, toEquatable);
        statistics.recordFileResult(REJECTED);
        resultStatistics = assertSameInstanceOnSubsequentInvocations(resultStatisticsSupplier, toEquatable);
        statistics.clear();
        assertSameInstanceOnSubsequentInvocationsAfterChange(resultStatistics, resultStatisticsSupplier, toEquatable);
    }

    @Test
    void getMessage_returns_same_instance_on_subsequent_invocations_between_changes() {
        ResultStatistics resultStatistics;
        final Supplier<ResultStatistics> resultStatisticsSupplier = () -> statistics.getMessage();
        final Function<ResultStatistics, Map<ProcessingResult, Integer>> toEquatable = ResultStatistics::asMap;

        resultStatistics = assertSameInstanceOnSubsequentInvocations(resultStatisticsSupplier, toEquatable);
        statistics.initMessages(2, 3);
        resultStatistics = assertSameInstanceOnSubsequentInvocationsAfterChange(resultStatistics, resultStatisticsSupplier, toEquatable);
        statistics.recordMessageResult(4, 5, ARCHIVED);
        assertSameInstanceOnSubsequentInvocationsAfterChange(resultStatistics, resultStatisticsSupplier, toEquatable);
        statistics.recordBulletinResult(6, DISCARDED);
        assertSameInstanceOnSubsequentInvocations(resultStatisticsSupplier, toEquatable);
        statistics.recordFileResult(REJECTED);
        resultStatistics = assertSameInstanceOnSubsequentInvocations(resultStatisticsSupplier, toEquatable);
        statistics.clear();
        assertSameInstanceOnSubsequentInvocationsAfterChange(resultStatistics, resultStatisticsSupplier, toEquatable);
    }

    @Test
    void initially_empty() {
        FileProcessingStatisticsSpec.assertEmpty(statistics);
    }

    @Test
    void clear_restores_initial_state() {
        statistics.recordFileResult(REJECTED);
        statistics.recordBulletinResult(0, DISCARDED);
        statistics.recordMessageResult(0, 0, ARCHIVED);

        statistics.clear();

        FileProcessingStatisticsSpec.assertEmpty(statistics);
    }

    @Test
    void initBulletins_does_nothing_when_amount_is_zero() {
        statistics.initBulletins(0);

        FileProcessingStatisticsSpec.assertEmpty(statistics);
    }

    @Test
    void initBulletins_does_nothing_when_amount_is_negative() {
        statistics.initBulletins(-1);

        FileProcessingStatisticsSpec.assertEmpty(statistics);
    }

    @Test
    void initBulletins_records_initial_processingResult_for_amount_of_bulletins() {
        statistics.initBulletins(3);

        FileProcessingStatisticsSpec.builder()//
                .putBulletin(NOTHING, 3)//
                .setBulletinTotal(3)//
                .setFile(NOTHING).setString("M{T:0} B{N:3,T:3} F{N}")//
                .assertEquals(statistics);
    }

    @Test
    void initMessages_does_nothing_when_bulletinIndex_is_negative() {
        statistics.initMessages(-1, 2);

        FileProcessingStatisticsSpec.assertEmpty(statistics);
    }

    @Test
    void initMessages_does_nothing_when_amount_is_zero() {
        statistics.initMessages(0, 0);

        FileProcessingStatisticsSpec.assertEmpty(statistics);
    }

    @Test
    void initMessages_does_nothing_when_amount_is_negative() {
        statistics.initMessages(0, -1);

        FileProcessingStatisticsSpec.assertEmpty(statistics);
    }

    @Test
    void initMessages_records_initial_processingResult_for_amount_of_messages() {
        statistics.initMessages(0, 4);

        FileProcessingStatisticsSpec.builder()//
                .putMessage(NOTHING, 4)//
                .setMessageTotal(4)//
                .putBulletin(NOTHING, 1)//
                .setBulletinTotal(1)//
                .setFile(NOTHING)//
                .setString("M{N:4,T:4} B{N:1,T:1} F{N}")//
                .assertEquals(statistics);
    }

    @Test
    void initMessages_implicitly_records_initial_processingResult_for_amount_for_bulletins_up_to_index() {
        statistics.initMessages(2, 4);

        FileProcessingStatisticsSpec.builder()//
                .putMessage(NOTHING, 4)//
                .setMessageTotal(4)//
                .putBulletin(NOTHING, 3)//
                .setBulletinTotal(3)//
                .setFile(NOTHING)//
                .setString("M{N:4,T:4} B{N:3,T:3} F{N}")//
                .assertEquals(statistics);
    }

    @Test
    void recordFileResult_records_file_processingResult() {
        statistics.recordFileResult(DISCARDED);

        FileProcessingStatisticsSpec.builder()//
                .setFile(DISCARDED)//
                .setString("M{T:0} B{T:0} F{D}")//
                .assertEquals(statistics);
    }

    @Test
    void recordFileResult_records_highest_file_processingResult() {
        final ProcessingResult highestProcessingResult = REJECTED;
        final ProcessingResult lastProcessingResult = DISCARDED;

        statistics.recordFileResult(ARCHIVED);
        statistics.recordFileResult(highestProcessingResult);
        statistics.recordFileResult(lastProcessingResult);

        assertThat(highestProcessingResult).usingComparator(ProcessingResult.getComparator()).isGreaterThan(lastProcessingResult);
        FileProcessingStatisticsSpec.builder()//
                .setFile(REJECTED)//
                .setString("M{T:0} B{T:0} F{R}")//
                .assertEquals(statistics);
    }

    @Test
    void recordBulletinResult_records_bulletin_processingResult() {
        statistics.recordBulletinResult(0, NOTHING);
        statistics.recordBulletinResult(1, NOTHING);
        statistics.recordBulletinResult(2, NOTHING);
        statistics.recordBulletinResult(3, NOTHING);
        statistics.recordBulletinResult(4, NOTHING);
        statistics.recordBulletinResult(5, ARCHIVED);
        statistics.recordBulletinResult(6, ARCHIVED);
        statistics.recordBulletinResult(7, ARCHIVED);
        statistics.recordBulletinResult(8, ARCHIVED);
        statistics.recordBulletinResult(9, DISCARDED);
        statistics.recordBulletinResult(10, DISCARDED);
        statistics.recordBulletinResult(11, DISCARDED);
        statistics.recordBulletinResult(12, REJECTED);
        statistics.recordBulletinResult(13, REJECTED);
        statistics.recordBulletinResult(14, FAILED);

        FileProcessingStatisticsSpec.builder()//
                .putBulletin(NOTHING, 5)//
                .putBulletin(ARCHIVED, 4)//
                .putBulletin(DISCARDED, 3)//
                .putBulletin(REJECTED, 2)//
                .putBulletin(FAILED, 1)//
                .setBulletinTotal(15)//
                .setFile(FAILED)//
                .setString("M{T:0} B{N:5,A:4,D:3,R:2,F:1,T:15} F{F}")//
                .assertEquals(statistics);
    }

    @Test
    void toString_returns_only_non_zero_bulletin_statistics() {
        statistics.recordBulletinResult(0, ARCHIVED);
        statistics.recordBulletinResult(1, ARCHIVED);

        FileProcessingStatisticsSpec.builder()//
                .putBulletin(ARCHIVED, 2)//
                .setBulletinTotal(2)//
                .setFile(ARCHIVED)//
                .setString("M{T:0} B{A:2,T:2} F{A}")//
                .assertEquals(statistics);
    }

    @Test
    void unrecorded_bulletin_processingResult_is_nothing() {
        statistics.recordBulletinResult(0, ARCHIVED);
        statistics.recordBulletinResult(4, ARCHIVED);

        FileProcessingStatisticsSpec.builder()//
                .putBulletin(NOTHING, 3)//
                .putBulletin(ARCHIVED, 2)//
                .setBulletinTotal(5)//
                .setFile(ARCHIVED)//
                .setString("M{T:0} B{N:3,A:2,T:5} F{A}")//
                .assertEquals(statistics);
    }

    @Test
    void recordBulletinResult_records_highest_bulletin_processingResult() {
        final ProcessingResult highestProcessingResult = REJECTED;
        final ProcessingResult lastProcessingResult = DISCARDED;

        statistics.recordBulletinResult(0, ARCHIVED);
        statistics.recordBulletinResult(1, ARCHIVED);
        statistics.recordBulletinResult(1, highestProcessingResult);
        statistics.recordBulletinResult(1, lastProcessingResult);

        assertThat(highestProcessingResult).usingComparator(ProcessingResult.getComparator()).isGreaterThan(lastProcessingResult);
        FileProcessingStatisticsSpec.builder()//
                .putBulletin(ARCHIVED, 1)//
                .putBulletin(REJECTED, 1)//
                .setBulletinTotal(2)//
                .setFile(REJECTED)//
                .setString("M{T:0} B{A:1,R:1,T:2} F{R}")//
                .assertEquals(statistics);
    }

    @Test
    void toString_returns_file_processingResult_as_recorded_when_greater_than_highest_recorded_bulletin_processingResult() {
        final ProcessingResult fileProcessingResult = FAILED;
        final ProcessingResult highestBulletinProcessingResult = REJECTED;

        statistics.recordFileResult(fileProcessingResult);
        statistics.recordBulletinResult(0, highestBulletinProcessingResult);
        statistics.recordBulletinResult(1, ARCHIVED);

        assertThat(fileProcessingResult).usingComparator(ProcessingResult.getComparator()).isGreaterThan(highestBulletinProcessingResult);
        FileProcessingStatisticsSpec.builder()//
                .putBulletin(ARCHIVED, 1)//
                .putBulletin(REJECTED, 1)//
                .setBulletinTotal(2)//
                .setFile(FAILED)//
                .setString("M{T:0} B{A:1,R:1,T:2} F{F}")//
                .assertEquals(statistics);
    }

    @Test
    void toString_returns_highest_recorded_bulletin_processingResult_as_file_processingResult_when_greater_than_recorded_file_processingResult() {
        final ProcessingResult fileProcessingResult = REJECTED;
        final ProcessingResult highestBulletinProcessingResult = FAILED;

        statistics.recordFileResult(fileProcessingResult);
        statistics.recordBulletinResult(0, highestBulletinProcessingResult);
        statistics.recordBulletinResult(1, ARCHIVED);

        assertThat(highestBulletinProcessingResult).usingComparator(ProcessingResult.getComparator()).isGreaterThan(fileProcessingResult);
        FileProcessingStatisticsSpec.builder()//
                .putBulletin(ARCHIVED, 1)//
                .putBulletin(FAILED, 1)//
                .setBulletinTotal(2)//
                .setFile(FAILED)//
                .setString("M{T:0} B{A:1,F:1,T:2} F{F}")//
                .assertEquals(statistics);
    }

    @Test
    void recordMessageResult_records_message_processingResult() {
        statistics.recordMessageResult(0, 0, NOTHING);
        statistics.recordMessageResult(0, 1, NOTHING);
        statistics.recordMessageResult(0, 2, NOTHING);
        statistics.recordMessageResult(0, 3, NOTHING);
        statistics.recordMessageResult(0, 4, NOTHING);
        statistics.recordMessageResult(0, 5, ARCHIVED);
        statistics.recordMessageResult(0, 6, ARCHIVED);
        statistics.recordMessageResult(0, 7, ARCHIVED);
        statistics.recordMessageResult(0, 8, ARCHIVED);
        statistics.recordMessageResult(0, 9, DISCARDED);
        statistics.recordMessageResult(0, 10, DISCARDED);
        statistics.recordMessageResult(0, 11, DISCARDED);
        statistics.recordMessageResult(0, 12, REJECTED);
        statistics.recordMessageResult(0, 13, REJECTED);
        statistics.recordMessageResult(0, 14, FAILED);

        FileProcessingStatisticsSpec.builder()//
                .putMessage(NOTHING, 5)//
                .putMessage(ARCHIVED, 4)//
                .putMessage(DISCARDED, 3)//
                .putMessage(REJECTED, 2)//
                .putMessage(FAILED, 1)//
                .setMessageTotal(15)//
                .putBulletin(FAILED, 1)//
                .setBulletinTotal(1)//
                .setFile(FAILED)//
                .setString("M{N:5,A:4,D:3,R:2,F:1,T:15} B{F:1,T:1} F{F}")//
                .assertEquals(statistics);
    }

    @Test
    void toString_returns_only_non_zero_message_statistics() {
        statistics.recordMessageResult(0, 0, ARCHIVED);
        statistics.recordMessageResult(0, 1, ARCHIVED);

        FileProcessingStatisticsSpec.builder()//
                .putMessage(ARCHIVED, 2)//
                .setMessageTotal(2)//
                .putBulletin(ARCHIVED, 1)//
                .setBulletinTotal(1)//
                .setFile(ARCHIVED)//
                .setString("M{A:2,T:2} B{A:1,T:1} F{A}")//
                .assertEquals(statistics);
    }

    @Test
    void unrecorded_message_processingResult_is_nothing() {
        statistics.recordMessageResult(0, 0, ARCHIVED);
        statistics.recordMessageResult(0, 4, ARCHIVED);
        statistics.recordMessageResult(1, 2, NOTHING);

        FileProcessingStatisticsSpec.builder()//
                .putMessage(NOTHING, 6)//
                .putMessage(ARCHIVED, 2)//
                .setMessageTotal(8)//
                .putBulletin(NOTHING, 1)//
                .putBulletin(ARCHIVED, 1)//
                .setBulletinTotal(2)//
                .setFile(ARCHIVED)//
                .setString("M{N:6,A:2,T:8} B{N:1,A:1,T:2} F{A}")//
                .assertEquals(statistics);
    }

    @Test
    void recordMessageResult_records_highest_message_processingResult() {
        final ProcessingResult highestProcessingResult = REJECTED;
        final ProcessingResult lastProcessingResult = DISCARDED;

        statistics.recordMessageResult(0, 0, ARCHIVED);
        statistics.recordMessageResult(0, 1, ARCHIVED);
        statistics.recordMessageResult(0, 1, highestProcessingResult);
        statistics.recordMessageResult(0, 1, lastProcessingResult);

        assertThat(highestProcessingResult).usingComparator(ProcessingResult.getComparator()).isGreaterThan(lastProcessingResult);
        FileProcessingStatisticsSpec.builder()//
                .putMessage(ARCHIVED, 1)//
                .putMessage(REJECTED, 1)//
                .setMessageTotal(2)//
                .putBulletin(REJECTED, 1)//
                .setBulletinTotal(1)//
                .setFile(REJECTED)//
                .setString("M{A:1,R:1,T:2} B{R:1,T:1} F{R}")//
                .assertEquals(statistics);
    }

    @Test
    void toString_returns_file_processingResult_as_recorded_when_greater_than_highest_recorded_bulletin_and_message_processingResult() {
        final ProcessingResult fileProcessingResult = FAILED;
        final ProcessingResult highestBulletinProcessingResult = REJECTED;
        final ProcessingResult highestMessageProcessingResult = DISCARDED;

        statistics.recordFileResult(fileProcessingResult);
        statistics.recordBulletinResult(0, highestBulletinProcessingResult);
        statistics.recordMessageResult(0, 0, ARCHIVED);
        statistics.recordMessageResult(0, 1, highestMessageProcessingResult);

        assertThat(fileProcessingResult).usingComparator(ProcessingResult.getComparator())//
                .isGreaterThan(highestBulletinProcessingResult)//
                .isGreaterThan(highestMessageProcessingResult);
        FileProcessingStatisticsSpec.builder()//
                .putMessage(ARCHIVED, 1)//
                .putMessage(DISCARDED, 1)//
                .setMessageTotal(2)//
                .putBulletin(REJECTED, 1)//
                .setBulletinTotal(1)//
                .setFile(FAILED)//
                .setString("M{A:1,D:1,T:2} B{R:1,T:1} F{F}")//
                .assertEquals(statistics);
    }

    @Test
    void toString_returns_highest_recorded_bulletin_processingResult_as_file_processingResult_when_greater_than_recorded_file_and_message_processingResult() {
        final ProcessingResult fileProcessingResult = REJECTED;
        final ProcessingResult highestBulletinProcessingResult = FAILED;
        final ProcessingResult highestMessageProcessingResult = DISCARDED;

        statistics.recordFileResult(fileProcessingResult);
        statistics.recordBulletinResult(0, highestBulletinProcessingResult);
        statistics.recordMessageResult(0, 0, ARCHIVED);
        statistics.recordMessageResult(0, 1, highestMessageProcessingResult);

        assertThat(highestBulletinProcessingResult).usingComparator(ProcessingResult.getComparator())//
                .isGreaterThan(fileProcessingResult)//
                .isGreaterThan(highestMessageProcessingResult);
        FileProcessingStatisticsSpec.builder()//
                .putMessage(ARCHIVED, 1)//
                .putMessage(DISCARDED, 1)//
                .setMessageTotal(2)//
                .putBulletin(FAILED, 1)//
                .setBulletinTotal(1)//
                .setFile(FAILED)//
                .setString("M{A:1,D:1,T:2} B{F:1,T:1} F{F}")//
                .assertEquals(statistics);
    }

    @Test
    void toString_returns_highest_recorded_message_processingResult_as_file_processingResult_when_greater_than_recorded_file_and_bulletin_processingResult() {
        final ProcessingResult fileProcessingResult = REJECTED;
        final ProcessingResult highestBulletinProcessingResult = DISCARDED;
        final ProcessingResult highestMessageProcessingResult = FAILED;

        statistics.recordFileResult(fileProcessingResult);
        statistics.recordBulletinResult(0, highestBulletinProcessingResult);
        statistics.recordMessageResult(0, 0, ARCHIVED);
        statistics.recordMessageResult(0, 1, highestMessageProcessingResult);

        assertThat(highestMessageProcessingResult).usingComparator(ProcessingResult.getComparator())//
                .isGreaterThan(fileProcessingResult)//
                .isGreaterThan(highestBulletinProcessingResult);
        FileProcessingStatisticsSpec.builder()//
                .putMessage(ARCHIVED, 1)//
                .putMessage(FAILED, 1)//
                .setMessageTotal(2)//
                .putBulletin(FAILED, 1)//
                .setBulletinTotal(1)//
                .setFile(FAILED)//
                .setString("M{A:1,F:1,T:2} B{F:1,T:1} F{F}")//
                .assertEquals(statistics);
    }

    @Test
    void toString_returns_highest_recorded_message_processingResult_as_bulletin_processingResult_when_greater_than_recorded_bulletin_processingResult() {
        final ProcessingResult highestBulletinProcessingResult = DISCARDED;
        final ProcessingResult highestMessageProcessingResult = REJECTED;

        statistics.recordBulletinResult(0, highestBulletinProcessingResult);
        statistics.recordMessageResult(0, 0, ARCHIVED);
        statistics.recordMessageResult(0, 1, highestMessageProcessingResult);

        assertThat(highestMessageProcessingResult).usingComparator(ProcessingResult.getComparator())//
                .isGreaterThan(highestBulletinProcessingResult);
        FileProcessingStatisticsSpec.builder()//
                .putMessage(ARCHIVED, 1)//
                .putMessage(REJECTED, 1)//
                .setMessageTotal(2)//
                .putBulletin(REJECTED, 1)//
                .setBulletinTotal(1)//
                .setFile(REJECTED)//
                .setString("M{A:1,R:1,T:2} B{R:1,T:1} F{R}")//
                .assertEquals(statistics);
    }

    @Test
    void computes_bulletin_and_file_statistics_from_message_states() {
        statistics.recordMessageResult(0, 0, ARCHIVED);
        statistics.recordMessageResult(0, 1, ARCHIVED);
        statistics.recordMessageResult(0, 2, ARCHIVED);
        statistics.recordMessageResult(1, 0, ARCHIVED);
        statistics.recordMessageResult(1, 1, ARCHIVED);
        statistics.recordMessageResult(1, 2, ARCHIVED);
        statistics.recordMessageResult(2, 0, ARCHIVED);
        statistics.recordMessageResult(2, 1, ARCHIVED);
        statistics.recordMessageResult(2, 2, ARCHIVED);
        statistics.recordMessageResult(3, 0, ARCHIVED);
        statistics.recordMessageResult(3, 1, ARCHIVED);
        statistics.recordMessageResult(3, 2, ARCHIVED);
        statistics.recordMessageResult(3, 3, DISCARDED);
        statistics.recordMessageResult(3, 4, DISCARDED);
        statistics.recordMessageResult(4, 0, ARCHIVED);
        statistics.recordMessageResult(4, 1, ARCHIVED);
        statistics.recordMessageResult(4, 2, ARCHIVED);
        statistics.recordMessageResult(4, 3, DISCARDED);
        statistics.recordMessageResult(4, 4, DISCARDED);
        statistics.recordMessageResult(5, 0, ARCHIVED);
        statistics.recordMessageResult(5, 1, ARCHIVED);
        statistics.recordMessageResult(5, 2, ARCHIVED);
        statistics.recordMessageResult(5, 3, DISCARDED);
        statistics.recordMessageResult(5, 4, DISCARDED);
        statistics.recordMessageResult(5, 5, REJECTED);

        FileProcessingStatisticsSpec.builder()//
                .putMessage(ARCHIVED, 18)//
                .putMessage(DISCARDED, 6)//
                .putMessage(REJECTED, 1)//
                .setMessageTotal(25)//
                .putBulletin(ARCHIVED, 3)//
                .putBulletin(DISCARDED, 2)//
                .putBulletin(REJECTED, 1)//
                .setBulletinTotal(6)//
                .setFile(REJECTED)//
                .setString("M{A:18,D:6,R:1,T:25} B{A:3,D:2,R:1,T:6} F{R}")//
                .assertEquals(statistics);
    }

    @Test
    void estimateLogStringLength_returns_decent_estimate_on_minimal_content() {
        final int fixedEstimate = 110;

        assertDecentLengthEstimate(statistics, length -> fixedEstimate);
    }

    @Test
    void estimateLogStringLength_returns_decent_estimate_on_long_content() {
        final int valuesCount = ProcessingResult.getValues().size();
        ProcessingResult.getValues().forEach(processingResult -> //
                IntStream.rangeClosed(1, 2000).forEach(occurrence -> {
                    final int baseIndex = occurrence * valuesCount;
                    statistics.recordMessageResult(0, baseIndex + processingResult.ordinal(), processingResult);
                    statistics.recordBulletinResult(baseIndex + processingResult.ordinal() + 1, processingResult);
                }));

        assertDecentLengthEstimate(statistics);
    }
}
