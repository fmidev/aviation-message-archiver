package fi.fmi.avi.archiver.logging;

import static fi.fmi.avi.archiver.logging.LoggableTests.assertDecentLengthEstimate;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fi.fmi.avi.archiver.logging.FileProcessingStatistics.Status;

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
        statistics.recordFileStatus(Status.REJECTED);
        statistics.recordBulletinStatus(0, Status.DISCARDED);
        statistics.recordMessageStatus(0, 0, Status.ARCHIVED);

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
    void initBulletins_records_initial_status_for_amount_of_bulletins() {
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
    void initMessages_records_initial_status_for_amount_of_messages() {
        statistics.initMessages(0, 4);

        assertThat(statistics.toString()).isEqualTo("M{N:4,T:4} B{N:1,T:1} F{N}");
    }

    @Test
    void initMessages_implicitly_records_initial_status_for_amount_for_bulletins_up_to_index() {
        statistics.initMessages(2, 4);

        assertThat(statistics.toString()).isEqualTo("M{N:4,T:4} B{N:3,T:3} F{N}");
    }

    @Test
    void recordFileStatus_records_file_status() {
        statistics.recordFileStatus(Status.DISCARDED);

        assertThat(statistics.toString()).isEqualTo("M{T:0} B{T:0} F{D}");
    }

    @Test
    void recordFileStatus_records_highest_file_status() {
        final Status highestStatus = Status.REJECTED;
        final Status lastStatus = Status.DISCARDED;

        statistics.recordFileStatus(Status.ARCHIVED);
        statistics.recordFileStatus(highestStatus);
        statistics.recordFileStatus(lastStatus);

        assertThat(highestStatus).usingComparator(Status.getComparator()).isGreaterThan(lastStatus);
        assertThat(statistics.toString()).isEqualTo("M{T:0} B{T:0} F{R}");
    }

    @Test
    void recordBulletinStatus_records_bulletin_status() {
        statistics.recordBulletinStatus(0, Status.NOTHING);
        statistics.recordBulletinStatus(1, Status.NOTHING);
        statistics.recordBulletinStatus(2, Status.NOTHING);
        statistics.recordBulletinStatus(3, Status.NOTHING);
        statistics.recordBulletinStatus(4, Status.NOTHING);
        statistics.recordBulletinStatus(5, Status.ARCHIVED);
        statistics.recordBulletinStatus(6, Status.ARCHIVED);
        statistics.recordBulletinStatus(7, Status.ARCHIVED);
        statistics.recordBulletinStatus(8, Status.ARCHIVED);
        statistics.recordBulletinStatus(9, Status.DISCARDED);
        statistics.recordBulletinStatus(10, Status.DISCARDED);
        statistics.recordBulletinStatus(11, Status.DISCARDED);
        statistics.recordBulletinStatus(12, Status.REJECTED);
        statistics.recordBulletinStatus(13, Status.REJECTED);
        statistics.recordBulletinStatus(14, Status.FAILED);

        assertThat(statistics.toString()).isEqualTo("M{T:0} B{N:5,A:4,D:3,R:2,F:1,T:15} F{F}");
    }

    @Test
    void toString_returns_only_non_zero_bulletin_statistics() {
        statistics.recordBulletinStatus(0, Status.ARCHIVED);
        statistics.recordBulletinStatus(1, Status.ARCHIVED);

        assertThat(statistics.toString()).isEqualTo("M{T:0} B{A:2,T:2} F{A}");
    }

    @Test
    void unrecorded_bulletin_status_is_nothing() {
        statistics.recordBulletinStatus(0, Status.ARCHIVED);
        statistics.recordBulletinStatus(4, Status.ARCHIVED);

        assertThat(statistics.toString()).isEqualTo("M{T:0} B{N:3,A:2,T:5} F{A}");
    }

    @Test
    void recordBulletinStatus_records_highest_bulletin_status() {
        final Status highestStatus = Status.REJECTED;
        final Status lastStatus = Status.DISCARDED;

        statistics.recordBulletinStatus(0, Status.ARCHIVED);
        statistics.recordBulletinStatus(1, Status.ARCHIVED);
        statistics.recordBulletinStatus(1, highestStatus);
        statistics.recordBulletinStatus(1, lastStatus);

        assertThat(highestStatus).usingComparator(Status.getComparator()).isGreaterThan(lastStatus);
        assertThat(statistics.toString()).isEqualTo("M{T:0} B{A:1,R:1,T:2} F{R}");
    }

    @Test
    void toString_returns_file_status_as_recorded_when_greater_than_highest_recorded_bulletin_status() {
        final Status fileStatus = Status.FAILED;
        final Status highestBulletinStatus = Status.REJECTED;

        statistics.recordFileStatus(fileStatus);
        statistics.recordBulletinStatus(0, highestBulletinStatus);
        statistics.recordBulletinStatus(1, Status.ARCHIVED);

        assertThat(fileStatus).usingComparator(Status.getComparator()).isGreaterThan(highestBulletinStatus);
        assertThat(statistics.toString()).isEqualTo("M{T:0} B{A:1,R:1,T:2} F{F}");
    }

    @Test
    void toString_returns_highest_recorded_bulletin_status_as_file_status_when_greater_than_recorded_file_status() {
        final Status fileStatus = Status.REJECTED;
        final Status highestBulletinStatus = Status.FAILED;

        statistics.recordFileStatus(fileStatus);
        statistics.recordBulletinStatus(0, highestBulletinStatus);
        statistics.recordBulletinStatus(1, Status.ARCHIVED);

        assertThat(highestBulletinStatus).usingComparator(Status.getComparator()).isGreaterThan(fileStatus);
        assertThat(statistics.toString()).isEqualTo("M{T:0} B{A:1,F:1,T:2} F{F}");
    }

    @Test
    void recordMessageStatus_records_message_status() {
        statistics.recordMessageStatus(0, 0, Status.NOTHING);
        statistics.recordMessageStatus(0, 1, Status.NOTHING);
        statistics.recordMessageStatus(0, 2, Status.NOTHING);
        statistics.recordMessageStatus(0, 3, Status.NOTHING);
        statistics.recordMessageStatus(0, 4, Status.NOTHING);
        statistics.recordMessageStatus(0, 5, Status.ARCHIVED);
        statistics.recordMessageStatus(0, 6, Status.ARCHIVED);
        statistics.recordMessageStatus(0, 7, Status.ARCHIVED);
        statistics.recordMessageStatus(0, 8, Status.ARCHIVED);
        statistics.recordMessageStatus(0, 9, Status.DISCARDED);
        statistics.recordMessageStatus(0, 10, Status.DISCARDED);
        statistics.recordMessageStatus(0, 11, Status.DISCARDED);
        statistics.recordMessageStatus(0, 12, Status.REJECTED);
        statistics.recordMessageStatus(0, 13, Status.REJECTED);
        statistics.recordMessageStatus(0, 14, Status.FAILED);

        assertThat(statistics.toString()).isEqualTo("M{N:5,A:4,D:3,R:2,F:1,T:15} B{F:1,T:1} F{F}");
    }

    @Test
    void toString_returns_only_non_zero_message_statistics() {
        statistics.recordMessageStatus(0, 0, Status.ARCHIVED);
        statistics.recordMessageStatus(0, 1, Status.ARCHIVED);

        assertThat(statistics.toString()).isEqualTo("M{A:2,T:2} B{A:1,T:1} F{A}");
    }

    @Test
    void unrecorded_message_status_is_nothing() {
        statistics.recordMessageStatus(0, 0, Status.ARCHIVED);
        statistics.recordMessageStatus(0, 4, Status.ARCHIVED);
        statistics.recordMessageStatus(1, 2, Status.NOTHING);

        assertThat(statistics.toString()).isEqualTo("M{N:6,A:2,T:8} B{N:1,A:1,T:2} F{A}");
    }

    @Test
    void recordMessageStatus_records_highest_message_status() {
        final Status highestStatus = Status.REJECTED;
        final Status lastStatus = Status.DISCARDED;

        statistics.recordMessageStatus(0, 0, Status.ARCHIVED);
        statistics.recordMessageStatus(0, 1, Status.ARCHIVED);
        statistics.recordMessageStatus(0, 1, highestStatus);
        statistics.recordMessageStatus(0, 1, lastStatus);

        assertThat(highestStatus).usingComparator(Status.getComparator()).isGreaterThan(lastStatus);
        assertThat(statistics.toString()).isEqualTo("M{A:1,R:1,T:2} B{R:1,T:1} F{R}");
    }

    @Test
    void toString_returns_file_status_as_recorded_when_greater_than_highest_recorded_bulletin_and_message_status() {
        final Status fileStatus = Status.FAILED;
        final Status highestBulletinStatus = Status.REJECTED;
        final Status highestMessageStatus = Status.DISCARDED;

        statistics.recordFileStatus(fileStatus);
        statistics.recordBulletinStatus(0, highestBulletinStatus);
        statistics.recordMessageStatus(0, 0, Status.ARCHIVED);
        statistics.recordMessageStatus(0, 1, highestMessageStatus);

        assertThat(fileStatus).usingComparator(Status.getComparator())//
                .isGreaterThan(highestBulletinStatus)//
                .isGreaterThan(highestMessageStatus);
        assertThat(statistics.toString()).isEqualTo("M{A:1,D:1,T:2} B{R:1,T:1} F{F}");
    }

    @Test
    void toString_returns_highest_recorded_bulletin_status_as_file_status_when_greater_than_recorded_file_and_message_status() {
        final Status fileStatus = Status.REJECTED;
        final Status highestBulletinStatus = Status.FAILED;
        final Status highestMessageStatus = Status.DISCARDED;

        statistics.recordFileStatus(fileStatus);
        statistics.recordBulletinStatus(0, highestBulletinStatus);
        statistics.recordMessageStatus(0, 0, Status.ARCHIVED);
        statistics.recordMessageStatus(0, 1, highestMessageStatus);

        assertThat(highestBulletinStatus).usingComparator(Status.getComparator())//
                .isGreaterThan(fileStatus)//
                .isGreaterThan(highestMessageStatus);
        assertThat(statistics.toString()).isEqualTo("M{A:1,D:1,T:2} B{F:1,T:1} F{F}");
    }

    @Test
    void toString_returns_highest_recorded_message_status_as_file_status_when_greater_than_recorded_file_and_bulletin_status() {
        final Status fileStatus = Status.REJECTED;
        final Status highestBulletinStatus = Status.DISCARDED;
        final Status highestMessageStatus = Status.FAILED;

        statistics.recordFileStatus(fileStatus);
        statistics.recordBulletinStatus(0, highestBulletinStatus);
        statistics.recordMessageStatus(0, 0, Status.ARCHIVED);
        statistics.recordMessageStatus(0, 1, highestMessageStatus);

        assertThat(highestMessageStatus).usingComparator(Status.getComparator())//
                .isGreaterThan(fileStatus)//
                .isGreaterThan(highestBulletinStatus);
        assertThat(statistics.toString()).isEqualTo("M{A:1,F:1,T:2} B{F:1,T:1} F{F}");
    }

    @Test
    void toString_returns_highest_recorded_message_status_as_bulletin_status_when_greater_than_recorded_bulletin_status() {
        final Status highestBulletinStatus = Status.DISCARDED;
        final Status highestMessageStatus = Status.REJECTED;

        statistics.recordBulletinStatus(0, highestBulletinStatus);
        statistics.recordMessageStatus(0, 0, Status.ARCHIVED);
        statistics.recordMessageStatus(0, 1, highestMessageStatus);

        assertThat(highestMessageStatus).usingComparator(Status.getComparator())//
                .isGreaterThan(highestBulletinStatus);
        assertThat(statistics.toString()).isEqualTo("M{A:1,R:1,T:2} B{R:1,T:1} F{R}");
    }

    @Test
    void computes_bulletin_and_file_statistics_from_message_states() {
        statistics.recordMessageStatus(0, 0, Status.ARCHIVED);
        statistics.recordMessageStatus(0, 1, Status.ARCHIVED);
        statistics.recordMessageStatus(0, 2, Status.ARCHIVED);
        statistics.recordMessageStatus(1, 0, Status.ARCHIVED);
        statistics.recordMessageStatus(1, 1, Status.ARCHIVED);
        statistics.recordMessageStatus(1, 2, Status.ARCHIVED);
        statistics.recordMessageStatus(2, 0, Status.ARCHIVED);
        statistics.recordMessageStatus(2, 1, Status.ARCHIVED);
        statistics.recordMessageStatus(2, 2, Status.ARCHIVED);
        statistics.recordMessageStatus(3, 0, Status.ARCHIVED);
        statistics.recordMessageStatus(3, 1, Status.ARCHIVED);
        statistics.recordMessageStatus(3, 2, Status.ARCHIVED);
        statistics.recordMessageStatus(3, 3, Status.DISCARDED);
        statistics.recordMessageStatus(3, 4, Status.DISCARDED);
        statistics.recordMessageStatus(4, 0, Status.ARCHIVED);
        statistics.recordMessageStatus(4, 1, Status.ARCHIVED);
        statistics.recordMessageStatus(4, 2, Status.ARCHIVED);
        statistics.recordMessageStatus(4, 3, Status.DISCARDED);
        statistics.recordMessageStatus(4, 4, Status.DISCARDED);
        statistics.recordMessageStatus(5, 0, Status.ARCHIVED);
        statistics.recordMessageStatus(5, 1, Status.ARCHIVED);
        statistics.recordMessageStatus(5, 2, Status.ARCHIVED);
        statistics.recordMessageStatus(5, 3, Status.DISCARDED);
        statistics.recordMessageStatus(5, 4, Status.DISCARDED);
        statistics.recordMessageStatus(5, 5, Status.REJECTED);

        assertThat(statistics.toString()).isEqualTo("M{A:18,D:6,R:1,T:25} B{A:3,D:2,R:1,T:6} F{R}");
    }

    @Test
    void estimateLogStringLength_returns_decent_estimate_on_minimal_content() {
        final int fixedEstimate = 96;

        assertDecentLengthEstimate(statistics, length -> fixedEstimate);
    }

    @Test
    void estimateLogStringLength_returns_decent_estimate_on_long_content() {
        final int valuesCount = Status.getValues().size();
        Status.getValues().forEach(status -> //
                IntStream.rangeClosed(1, 2000).forEach(occurrence -> {
                    final int baseIndex = occurrence * valuesCount;
                    statistics.recordMessageStatus(0, baseIndex + status.ordinal(), status);
                    statistics.recordBulletinStatus(baseIndex + status.ordinal() + 1, status);
                }));

        assertDecentLengthEstimate(statistics);
    }
}
