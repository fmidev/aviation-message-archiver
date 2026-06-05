package fi.fmi.avi.archiver;

import com.google.common.testing.AbstractPackageSanityTests;
import fi.fmi.avi.archiver.util.GeneratedClasses;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

public class PackageSanityTest extends AbstractPackageSanityTests {
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        ignoreClasses(GeneratedClasses::isKnownGenerated);

        setDefault(Clock.class, Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));
    }
}
