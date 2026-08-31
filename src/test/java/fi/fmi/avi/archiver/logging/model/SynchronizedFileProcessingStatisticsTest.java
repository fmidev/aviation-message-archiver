package fi.fmi.avi.archiver.logging.model;

import com.google.common.testing.ForwardingWrapperTester;
import org.junit.jupiter.api.Test;

class SynchronizedFileProcessingStatisticsTest {
    @Test
    void testForwarding() {
        new ForwardingWrapperTester().testForwarding(FileProcessingStatistics.class, SynchronizedFileProcessingStatistics::new);
    }
}
