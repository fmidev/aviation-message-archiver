package fi.fmi.avi.archiver.logging.model;

import org.junit.jupiter.api.Test;

import com.google.common.testing.ForwardingWrapperTester;

@SuppressWarnings("UnstableApiUsage")
class SynchronizedFileProcessingStatisticsTest {
    @Test
    void testForwarding() {
        new ForwardingWrapperTester().testForwarding(FileProcessingStatistics.class, SynchronizedFileProcessingStatistics::new);
    }
}
