package fi.fmi.avi.archiver;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.threeten.extra.MutableClock;

import fi.fmi.avi.archiver.file.FileReference;

class ProcessingStateTest {
    private static final FileReference FILE_REFERENCE_1 = FileReference.create("productid", "filename1.txt");
    private static final FileReference FILE_REFERENCE_2 = FileReference.create("productid", "filename2.txt");
    private static final Clock FIXED_EPOCH_CLOCK = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC);

    @Test
    void fileCountUnderProcessing_is_initially_zero() {
        final ProcessingState processingState = new ProcessingState(FIXED_EPOCH_CLOCK);

        assertThat(processingState.getFileCountUnderProcessing()).isEqualTo(0);
    }

    @Test
    void getRunningFileProcessingMaxElapsed_returns_initially_zero() {
        final ProcessingState processingState = new ProcessingState(FIXED_EPOCH_CLOCK);

        assertThat(processingState.getRunningFileProcessingMaxElapsed()).isEqualTo(Duration.ZERO);
    }

    @Test
    void start_FileMetadata_increases_fileCountUnderProcessing() {
        final ProcessingState processingState = new ProcessingState(FIXED_EPOCH_CLOCK);

        processingState.start(FILE_REFERENCE_1);

        assertThat(processingState.getFileCountUnderProcessing()).isEqualTo(1);
    }

    @Test
    void finish_on_unprocessed_file_does_nothing() {
        final ProcessingState processingState = new ProcessingState(FIXED_EPOCH_CLOCK);

        processingState.finish(FILE_REFERENCE_1);
        assertThat(processingState.getFileCountUnderProcessing()).isEqualTo(0);
    }

    @Test
    void finish_on_processed_file_does_nothing() {
        final ProcessingState processingState = new ProcessingState(FIXED_EPOCH_CLOCK);

        processingState.start(FILE_REFERENCE_1);
        processingState.finish(FILE_REFERENCE_1);
        assertThat(processingState.getFileCountUnderProcessing()).as("finished").isEqualTo(0);
        processingState.finish(FILE_REFERENCE_1);
        assertThat(processingState.getFileCountUnderProcessing()).as("second invocation").isEqualTo(0);
    }

    @Test
    void finish_FileMetadata_decreases_fileCountUnderProcessing() {
        final ProcessingState processingState = new ProcessingState(FIXED_EPOCH_CLOCK);

        processingState.start(FILE_REFERENCE_1);
        processingState.start(FILE_REFERENCE_2);
        assertThat(processingState.getFileCountUnderProcessing()).isEqualTo(2);
        processingState.finish(FILE_REFERENCE_1);

        assertThat(processingState.getFileCountUnderProcessing()).isEqualTo(1);
    }

    @Test
    void repeated_start_FileMetadata_requires_repeated_finish_to_decrease_count() {
        final ProcessingState processingState = new ProcessingState(FIXED_EPOCH_CLOCK);

        processingState.start(FILE_REFERENCE_1);
        assertThat(processingState.getFileCountUnderProcessing()).isEqualTo(1);
        processingState.start(FILE_REFERENCE_2);
        assertThat(processingState.getFileCountUnderProcessing()).isEqualTo(2);
        processingState.start(FILE_REFERENCE_1);
        assertThat(processingState.getFileCountUnderProcessing()).isEqualTo(2);
        processingState.start(FILE_REFERENCE_1);
        assertThat(processingState.getFileCountUnderProcessing()).isEqualTo(2);

        processingState.finish(FILE_REFERENCE_1);
        assertThat(processingState.getFileCountUnderProcessing()).isEqualTo(2);
        processingState.finish(FILE_REFERENCE_2);
        assertThat(processingState.getFileCountUnderProcessing()).isEqualTo(1);
        processingState.finish(FILE_REFERENCE_1);
        assertThat(processingState.getFileCountUnderProcessing()).isEqualTo(1);
        processingState.finish(FILE_REFERENCE_1);
        assertThat(processingState.getFileCountUnderProcessing()).isEqualTo(0);
    }

    @Test
    void getRunningFileProcessingMaxElapsed_returns_maximum_time_elapsed_from_start_of_first_running_task() {
        final MutableClock clock = MutableClock.of(FIXED_EPOCH_CLOCK.instant(), FIXED_EPOCH_CLOCK.getZone());
        final Duration timeAdvance = Duration.ofSeconds(10);
        final ProcessingState processingState = new ProcessingState(clock);

        processingState.start(FILE_REFERENCE_1);
        clock.add(timeAdvance);
        processingState.start(FILE_REFERENCE_2);
        clock.add(timeAdvance);
        processingState.start(FILE_REFERENCE_1);
        clock.add(timeAdvance);
        final Duration result = processingState.getRunningFileProcessingMaxElapsed();

        assertThat(result).isEqualTo(timeAdvance.multipliedBy(3));
    }

    @Test
    void isFileUnderProcessing_returns_wheter_file_is_under_processing() {
        final ProcessingState processingState = new ProcessingState(FIXED_EPOCH_CLOCK);

        assertThat(processingState.isFileUnderProcessing(FILE_REFERENCE_1)).as("1: initial").isFalse();
        assertThat(processingState.isFileUnderProcessing(FILE_REFERENCE_2)).as("2: initial").isFalse();
        processingState.start(FILE_REFERENCE_1);
        assertThat(processingState.isFileUnderProcessing(FILE_REFERENCE_1)).as("1: started 1").isTrue();
        assertThat(processingState.isFileUnderProcessing(FILE_REFERENCE_2)).as("2: started 1").isFalse();
        processingState.start(FILE_REFERENCE_2);
        assertThat(processingState.isFileUnderProcessing(FILE_REFERENCE_1)).as("1: started 2").isTrue();
        assertThat(processingState.isFileUnderProcessing(FILE_REFERENCE_2)).as("2: started 2").isTrue();
        processingState.finish(FILE_REFERENCE_1);
        assertThat(processingState.isFileUnderProcessing(FILE_REFERENCE_1)).as("1: finished 1").isFalse();
        assertThat(processingState.isFileUnderProcessing(FILE_REFERENCE_2)).as("2: finished 1").isTrue();
        processingState.finish(FILE_REFERENCE_2);
        assertThat(processingState.isFileUnderProcessing(FILE_REFERENCE_1)).as("1: finished 2").isFalse();
        assertThat(processingState.isFileUnderProcessing(FILE_REFERENCE_2)).as("2: finished 2").isFalse();
    }
}
