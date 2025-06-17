package fi.fmi.avi.archiver.spring.healthcontributor;

import com.google.common.testing.AbstractPackageSanityTests;
import fi.fmi.avi.archiver.spring.integration.util.MonitorableCallerBlocksPolicy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.mockito.Mockito.mock;

public class PackageSanityTest extends AbstractPackageSanityTests {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        setDefault(Clock.class, Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));
        setDefault(MonitorableCallerBlocksPolicy.class, mock(MonitorableCallerBlocksPolicy.class));
    }

}
