package fi.fmi.avi.archiver.logging;

import static fi.fmi.avi.archiver.logging.LoggableTests.assertDecentLengthEstimate;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fi.fmi.avi.archiver.logging.FileProcessingStatistics.ProcessingResult;

class FileProcessingStatisticsImplTest {

    private FileProcessingStatisticsImpl statistics;

    private static void assertEmpty(final FileProcessingStatisticsImpl statistics) {
        assertThat(statistics.toString()).isEqualTo("M{T:0} B{T:0} F{N}");
    }

    @BeforeEach
    void setUp() {
        statistics = new FileProcessingStatisticsImpl();
    }

    @Test
    void initially_empty() {
        assertEmpty(statistics);
    }

    @Test
    void clear_restores_initial_state() {
        statistics.recordFileResult(ProcessingResult.REJECTED);
        statistics.recordBulletinResult(0, ProcessingResult.DISCARDED);
        statistics.recordMessageResult(0, 0, ProcessingResult.ARCHIVED);

        statistics.clear();

        assertEmpty(statistics);
    }

    @Test
    void initBulletins_does_nothing_when_amount_is_zero() {
        statistics.initBulletins(0);

        assertEmpty(statistics);
    }

    @Test
    void initBulletins_does_nothing_when_amount_is_negative() {
        statistics.initBulletins(-1);

        assertEmpty(statistics);
    }

    @Test
    void initBulletins_records_initial_processingResult_for_amount_of_bulletins() {
        statistics.initBulletins(3);

        assertThat(statistics.toString()).isEqualTo("M{T:0} B{N:3,T:3} F{N}");
    }

    @Test
    void initMessages_does_nothing_when_bulletinIndex_is_negative() {
        statistics.initMessages(-1, 2);

        assertEmpty(statistics);
    }

    @Test
    void initMessages_does_nothing_when_amount_is_zero() {
        statistics.initMessages(0, 0);

        assertEmpty(statistics);
    }

    @Test
    void initMessages_does_nothing_when_amount_is_negative() {
        statistics.initMessages(0, -1);

        assertEmpty(statistics);
    }

    @Test
    void initMessages_records_initial_processingResult_for_amount_of_messages() {
        statistics.initMessages(0, 4);

        assertThat(statistics.toString()).isEqualTo("M{N:4,T:4} B{N:1,T:1} F{N}");
    }

    @Test
    void initMessages_implicitly_records_initial_processingResult_for_amount_for_bulletins_up_to_index() {
        statistics.initMessages(2, 4);

        assertThat(statistics.toString()).isEqualTo("M{N:4,T:4} B{N:3,T:3} F{N}");
    }

    @Test
    void recordFileResult_records_file_processingResult() {
        statistics.recordFileResult(ProcessingResult.DISCARDED);

        assertThat(statistics.toString()).isEqualTo("M{T:0} B{T:0} F{D}");
    }

    @Test
    void recordFileResult_records_highest_file_processingResult() {
        final ProcessingResult highestProcessingResult = ProcessingResult.REJECTED;
        final ProcessingResult lastProcessingResult = ProcessingResult.DISCARDED;

        statistics.recordFileResult(ProcessingResult.ARCHIVED);
        statistics.recordFileResult(highestProcessingResult);
        statistics.recordFileResult(lastProcessingResult);

        assertThat(highestProcessingResult).usingComparator(ProcessingResult.getComparator()).isGreaterThan(lastProcessingResult);
        assertThat(statistics.toString()).isEqualTo("M{T:0} B{T:0} F{R}");
    }

    @Test
    void recordBulletinResult_records_bulletin_processingResult() {
        statistics.recordBulletinResult(0, ProcessingResult.NOTHING);
        statistics.recordBulletinResult(1, ProcessingResult.NOTHING);
        statistics.recordBulletinResult(2, ProcessingResult.NOTHING);
        statistics.recordBulletinResult(3, ProcessingResult.NOTHING);
        statistics.recordBulletinResult(4, ProcessingResult.NOTHING);
        statistics.recordBulletinResult(5, ProcessingResult.ARCHIVED);
        statistics.recordBulletinResult(6, ProcessingResult.ARCHIVED);
        statistics.recordBulletinResult(7, ProcessingResult.ARCHIVED);
        statistics.recordBulletinResult(8, ProcessingResult.ARCHIVED);
        statistics.recordBulletinResult(9, ProcessingResult.DISCARDED);
        statistics.recordBulletinResult(10, ProcessingResult.DISCARDED);
        statistics.recordBulletinResult(11, ProcessingResult.DISCARDED);
        statistics.recordBulletinResult(12, ProcessingResult.REJECTED);
        statistics.recordBulletinResult(13, ProcessingResult.REJECTED);
        statistics.recordBulletinResult(14, ProcessingResult.FAILED);

        assertThat(statistics.toString()).isEqualTo("M{T:0} B{N:5,A:4,D:3,R:2,F:1,T:15} F{F}");
    }

    @Test
    void toString_returns_only_non_zero_bulletin_statistics() {
        statistics.recordBulletinResult(0, ProcessingResult.ARCHIVED);
        statistics.recordBulletinResult(1, ProcessingResult.ARCHIVED);

        assertThat(statistics.toString()).isEqualTo("M{T:0} B{A:2,T:2} F{A}");
    }

    @Test
    void unrecorded_bulletin_processingResult_is_nothing() {
        statistics.recordBulletinResult(0, ProcessingResult.ARCHIVED);
        statistics.recordBulletinResult(4, ProcessingResult.ARCHIVED);

        assertThat(statistics.toString()).isEqualTo("M{T:0} B{N:3,A:2,T:5} F{A}");
    }

    @Test
    void recordBulletinResult_records_highest_bulletin_processingResult() {
        final ProcessingResult highestProcessingResult = ProcessingResult.REJECTED;
        final ProcessingResult lastProcessingResult = ProcessingResult.DISCARDED;

        statistics.recordBulletinResult(0, ProcessingResult.ARCHIVED);
        statistics.recordBulletinResult(1, ProcessingResult.ARCHIVED);
        statistics.recordBulletinResult(1, highestProcessingResult);
        statistics.recordBulletinResult(1, lastProcessingResult);

        assertThat(highestProcessingResult).usingComparator(ProcessingResult.getComparator()).isGreaterThan(lastProcessingResult);
        assertThat(statistics.toString()).isEqualTo("M{T:0} B{A:1,R:1,T:2} F{R}");
    }

    @Test
    void toString_returns_file_processingResult_as_recorded_when_greater_than_highest_recorded_bulletin_processingResult() {
        final ProcessingResult fileProcessingResult = ProcessingResult.FAILED;
        final ProcessingResult highestBulletinProcessingResult = ProcessingResult.REJECTED;

        statistics.recordFileResult(fileProcessingResult);
        statistics.recordBulletinResult(0, highestBulletinProcessingResult);
        statistics.recordBulletinResult(1, ProcessingResult.ARCHIVED);

        assertThat(fileProcessingResult).usingComparator(ProcessingResult.getComparator()).isGreaterThan(highestBulletinProcessingResult);
        assertThat(statistics.toString()).isEqualTo("M{T:0} B{A:1,R:1,T:2} F{F}");
    }

    @Test
    void toString_returns_highest_recorded_bulletin_processingResult_as_file_processingResult_when_greater_than_recorded_file_processingResult() {
        final ProcessingResult fileProcessingResult = ProcessingResult.REJECTED;
        final ProcessingResult highestBulletinProcessingResult = ProcessingResult.FAILED;

        statistics.recordFileResult(fileProcessingResult);
        statistics.recordBulletinResult(0, highestBulletinProcessingResult);
        statistics.recordBulletinResult(1, ProcessingResult.ARCHIVED);

        assertThat(highestBulletinProcessingResult).usingComparator(ProcessingResult.getComparator()).isGreaterThan(fileProcessingResult);
        assertThat(statistics.toString()).isEqualTo("M{T:0} B{A:1,F:1,T:2} F{F}");
    }

    @Test
    void recordMessageResult_records_message_processingResult() {
        statistics.recordMessageResult(0, 0, ProcessingResult.NOTHING);
        statistics.recordMessageResult(0, 1, ProcessingResult.NOTHING);
        statistics.recordMessageResult(0, 2, ProcessingResult.NOTHING);
        statistics.recordMessageResult(0, 3, ProcessingResult.NOTHING);
        statistics.recordMessageResult(0, 4, ProcessingResult.NOTHING);
        statistics.recordMessageResult(0, 5, ProcessingResult.ARCHIVED);
        statistics.recordMessageResult(0, 6, ProcessingResult.ARCHIVED);
        statistics.recordMessageResult(0, 7, ProcessingResult.ARCHIVED);
        statistics.recordMessageResult(0, 8, ProcessingResult.ARCHIVED);
        statistics.recordMessageResult(0, 9, ProcessingResult.DISCARDED);
        statistics.recordMessageResult(0, 10, ProcessingResult.DISCARDED);
        statistics.recordMessageResult(0, 11, ProcessingResult.DISCARDED);
        statistics.recordMessageResult(0, 12, ProcessingResult.REJECTED);
        statistics.recordMessageResult(0, 13, ProcessingResult.REJECTED);
        statistics.recordMessageResult(0, 14, ProcessingResult.FAILED);

        assertThat(statistics.toString()).isEqualTo("M{N:5,A:4,D:3,R:2,F:1,T:15} B{F:1,T:1} F{F}");
    }

    @Test
    void toString_returns_only_non_zero_message_statistics() {
        statistics.recordMessageResult(0, 0, ProcessingResult.ARCHIVED);
        statistics.recordMessageResult(0, 1, ProcessingResult.ARCHIVED);

        assertThat(statistics.toString()).isEqualTo("M{A:2,T:2} B{A:1,T:1} F{A}");
    }

    @Test
    void unrecorded_message_processingResult_is_nothing() {
        statistics.recordMessageResult(0, 0, ProcessingResult.ARCHIVED);
        statistics.recordMessageResult(0, 4, ProcessingResult.ARCHIVED);
        statistics.recordMessageResult(1, 2, ProcessingResult.NOTHING);

        assertThat(statistics.toString()).isEqualTo("M{N:6,A:2,T:8} B{N:1,A:1,T:2} F{A}");
    }

    @Test
    void recordMessageResult_records_highest_message_processingResult() {
        final ProcessingResult highestProcessingResult = ProcessingResult.REJECTED;
        final ProcessingResult lastProcessingResult = ProcessingResult.DISCARDED;

        statistics.recordMessageResult(0, 0, ProcessingResult.ARCHIVED);
        statistics.recordMessageResult(0, 1, ProcessingResult.ARCHIVED);
        statistics.recordMessageResult(0, 1, highestProcessingResult);
        statistics.recordMessageResult(0, 1, lastProcessingResult);

        assertThat(highestProcessingResult).usingComparator(ProcessingResult.getComparator()).isGreaterThan(lastProcessingResult);
        assertThat(statistics.toString()).isEqualTo("M{A:1,R:1,T:2} B{R:1,T:1} F{R}");
    }

    @Test
    void toString_returns_file_processingResult_as_recorded_when_greater_than_highest_recorded_bulletin_and_message_processingResult() {
        final ProcessingResult fileProcessingResult = ProcessingResult.FAILED;
        final ProcessingResult highestBulletinProcessingResult = ProcessingResult.REJECTED;
        final ProcessingResult highestMessageProcessingResult = ProcessingResult.DISCARDED;

        statistics.recordFileResult(fileProcessingResult);
        statistics.recordBulletinResult(0, highestBulletinProcessingResult);
        statistics.recordMessageResult(0, 0, ProcessingResult.ARCHIVED);
        statistics.recordMessageResult(0, 1, highestMessageProcessingResult);

        assertThat(fileProcessingResult).usingComparator(ProcessingResult.getComparator())//
                .isGreaterThan(highestBulletinProcessingResult)//
                .isGreaterThan(highestMessageProcessingResult);
        assertThat(statistics.toString()).isEqualTo("M{A:1,D:1,T:2} B{R:1,T:1} F{F}");
    }

    @Test
    void toString_returns_highest_recorded_bulletin_processingResult_as_file_processingResult_when_greater_than_recorded_file_and_message_processingResult() {
        final ProcessingResult fileProcessingResult = ProcessingResult.REJECTED;
        final ProcessingResult highestBulletinProcessingResult = ProcessingResult.FAILED;
        final ProcessingResult highestMessageProcessingResult = ProcessingResult.DISCARDED;

        statistics.recordFileResult(fileProcessingResult);
        statistics.recordBulletinResult(0, highestBulletinProcessingResult);
        statistics.recordMessageResult(0, 0, ProcessingResult.ARCHIVED);
        statistics.recordMessageResult(0, 1, highestMessageProcessingResult);

        assertThat(highestBulletinProcessingResult).usingComparator(ProcessingResult.getComparator())//
                .isGreaterThan(fileProcessingResult)//
                .isGreaterThan(highestMessageProcessingResult);
        assertThat(statistics.toString()).isEqualTo("M{A:1,D:1,T:2} B{F:1,T:1} F{F}");
    }

    @Test
    void toString_returns_highest_recorded_message_processingResult_as_file_processingResult_when_greater_than_recorded_file_and_bulletin_processingResult() {
        final ProcessingResult fileProcessingResult = ProcessingResult.REJECTED;
        final ProcessingResult highestBulletinProcessingResult = ProcessingResult.DISCARDED;
        final ProcessingResult highestMessageProcessingResult = ProcessingResult.FAILED;

        statistics.recordFileResult(fileProcessingResult);
        statistics.recordBulletinResult(0, highestBulletinProcessingResult);
        statistics.recordMessageResult(0, 0, ProcessingResult.ARCHIVED);
        statistics.recordMessageResult(0, 1, highestMessageProcessingResult);

        assertThat(highestMessageProcessingResult).usingComparator(ProcessingResult.getComparator())//
                .isGreaterThan(fileProcessingResult)//
                .isGreaterThan(highestBulletinProcessingResult);
        assertThat(statistics.toString()).isEqualTo("M{A:1,F:1,T:2} B{F:1,T:1} F{F}");
    }

    @Test
    void toString_returns_highest_recorded_message_processingResult_as_bulletin_processingResult_when_greater_than_recorded_bulletin_processingResult() {
        final ProcessingResult highestBulletinProcessingResult = ProcessingResult.DISCARDED;
        final ProcessingResult highestMessageProcessingResult = ProcessingResult.REJECTED;

        statistics.recordBulletinResult(0, highestBulletinProcessingResult);
        statistics.recordMessageResult(0, 0, ProcessingResult.ARCHIVED);
        statistics.recordMessageResult(0, 1, highestMessageProcessingResult);

        assertThat(highestMessageProcessingResult).usingComparator(ProcessingResult.getComparator())//
                .isGreaterThan(highestBulletinProcessingResult);
        assertThat(statistics.toString()).isEqualTo("M{A:1,R:1,T:2} B{R:1,T:1} F{R}");
    }

    @Test
    void computes_bulletin_and_file_statistics_from_message_states() {
        statistics.recordMessageResult(0, 0, ProcessingResult.ARCHIVED);
        statistics.recordMessageResult(0, 1, ProcessingResult.ARCHIVED);
        statistics.recordMessageResult(0, 2, ProcessingResult.ARCHIVED);
        statistics.recordMessageResult(1, 0, ProcessingResult.ARCHIVED);
        statistics.recordMessageResult(1, 1, ProcessingResult.ARCHIVED);
        statistics.recordMessageResult(1, 2, ProcessingResult.ARCHIVED);
        statistics.recordMessageResult(2, 0, ProcessingResult.ARCHIVED);
        statistics.recordMessageResult(2, 1, ProcessingResult.ARCHIVED);
        statistics.recordMessageResult(2, 2, ProcessingResult.ARCHIVED);
        statistics.recordMessageResult(3, 0, ProcessingResult.ARCHIVED);
        statistics.recordMessageResult(3, 1, ProcessingResult.ARCHIVED);
        statistics.recordMessageResult(3, 2, ProcessingResult.ARCHIVED);
        statistics.recordMessageResult(3, 3, ProcessingResult.DISCARDED);
        statistics.recordMessageResult(3, 4, ProcessingResult.DISCARDED);
        statistics.recordMessageResult(4, 0, ProcessingResult.ARCHIVED);
        statistics.recordMessageResult(4, 1, ProcessingResult.ARCHIVED);
        statistics.recordMessageResult(4, 2, ProcessingResult.ARCHIVED);
        statistics.recordMessageResult(4, 3, ProcessingResult.DISCARDED);
        statistics.recordMessageResult(4, 4, ProcessingResult.DISCARDED);
        statistics.recordMessageResult(5, 0, ProcessingResult.ARCHIVED);
        statistics.recordMessageResult(5, 1, ProcessingResult.ARCHIVED);
        statistics.recordMessageResult(5, 2, ProcessingResult.ARCHIVED);
        statistics.recordMessageResult(5, 3, ProcessingResult.DISCARDED);
        statistics.recordMessageResult(5, 4, ProcessingResult.DISCARDED);
        statistics.recordMessageResult(5, 5, ProcessingResult.REJECTED);

        assertThat(statistics.toString()).isEqualTo("M{A:18,D:6,R:1,T:25} B{A:3,D:2,R:1,T:6} F{R}");
    }

    @Test
    void estimateLogStringLength_returns_decent_estimate_on_minimal_content() {
        final int fixedEstimate = 96;

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
