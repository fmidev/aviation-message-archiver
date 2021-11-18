package fi.fmi.avi.archiver;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import com.google.common.testing.AbstractPackageSanityTests;

public class PackageSanityTest extends AbstractPackageSanityTests {
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        setDefault(AviationMessageArchiverTest.AviationMessageArchiverTestCase.class,
                AviationMessageArchiverTest.AviationMessageArchiverTestCase.builder().buildPartial());
        setDefault(Clock.class, Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));
    }
}
