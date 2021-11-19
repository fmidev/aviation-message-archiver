package fi.fmi.avi.archiver;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.threeten.extra.MutableClock;

import fi.fmi.avi.archiver.file.FileMetadata;

class ProcessingMetricsTest {
    private static final FileMetadata FILE_METADATA_1 = FileMetadata.builder()//
            .setProductIdentifier("productid")//
            .setFilename("filename1.txt")//
            .buildPartial();
    private static final FileMetadata FILE_METADATA_2 = FileMetadata.builder()//
            .setProductIdentifier("productid")//
            .setFilename("filename2.txt")//
            .buildPartial();
    private static final Clock FIXED_EPOCH_CLOCK = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC);

    @Test
    void fileCountUnderProcessing_is_initially_zero() {
        final ProcessingMetrics metrics = new ProcessingMetrics(FIXED_EPOCH_CLOCK);

        assertThat(metrics.getFileCountUnderProcessing()).isEqualTo(0);
    }

    @Test
    void getRunningFileProcessingMaxElapsed_returns_initially_zero() {
        final ProcessingMetrics metrics = new ProcessingMetrics(FIXED_EPOCH_CLOCK);

        assertThat(metrics.getRunningFileProcessingMaxElapsed()).isEqualTo(Duration.ZERO);
    }

    @Test
    void start_FileMetadata_increases_fileCountUnderProcessing() {
        final ProcessingMetrics metrics = new ProcessingMetrics(FIXED_EPOCH_CLOCK);

        metrics.start(FILE_METADATA_1);

        assertThat(metrics.getFileCountUnderProcessing()).isEqualTo(1);
    }

    @Test
    void finish_FileMetadata_decreases_fileCountUnderProcessing() {
        final ProcessingMetrics metrics = new ProcessingMetrics(FIXED_EPOCH_CLOCK);

        metrics.start(FILE_METADATA_1);
        metrics.start(FILE_METADATA_2);
        assertThat(metrics.getFileCountUnderProcessing()).isEqualTo(2);
        metrics.finish(FILE_METADATA_1);

        assertThat(metrics.getFileCountUnderProcessing()).isEqualTo(1);
    }

    @Test
    void repeated_start_FileMetadata_requires_repeated_finish_to_decrease_count() {
        final ProcessingMetrics metrics = new ProcessingMetrics(FIXED_EPOCH_CLOCK);

        metrics.start(FILE_METADATA_1);
        assertThat(metrics.getFileCountUnderProcessing()).isEqualTo(1);
        metrics.start(FILE_METADATA_2);
        assertThat(metrics.getFileCountUnderProcessing()).isEqualTo(2);
        metrics.start(FILE_METADATA_1);
        assertThat(metrics.getFileCountUnderProcessing()).isEqualTo(2);
        metrics.start(FILE_METADATA_1);
        assertThat(metrics.getFileCountUnderProcessing()).isEqualTo(2);

        metrics.finish(FILE_METADATA_1);
        assertThat(metrics.getFileCountUnderProcessing()).isEqualTo(2);
        metrics.finish(FILE_METADATA_2);
        assertThat(metrics.getFileCountUnderProcessing()).isEqualTo(1);
        metrics.finish(FILE_METADATA_1);
        assertThat(metrics.getFileCountUnderProcessing()).isEqualTo(1);
        metrics.finish(FILE_METADATA_1);
        assertThat(metrics.getFileCountUnderProcessing()).isEqualTo(0);
    }

    @Test
    void getRunningFileProcessingMaxElapsed_returns_maximum_time_elapsed_from_start_of_first_running_task() {
        final MutableClock clock = MutableClock.of(FIXED_EPOCH_CLOCK.instant(), FIXED_EPOCH_CLOCK.getZone());
        final Duration timeAdvance = Duration.ofSeconds(10);
        final ProcessingMetrics metrics = new ProcessingMetrics(clock);

        metrics.start(FILE_METADATA_1);
        clock.add(timeAdvance);
        metrics.start(FILE_METADATA_2);
        clock.add(timeAdvance);
        metrics.start(FILE_METADATA_1);
        clock.add(timeAdvance);
        final Duration result = metrics.getRunningFileProcessingMaxElapsed();

        assertThat(result).isEqualTo(timeAdvance.multipliedBy(3));
    }
}
