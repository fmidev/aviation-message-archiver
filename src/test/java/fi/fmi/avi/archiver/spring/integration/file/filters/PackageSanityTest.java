package fi.fmi.avi.archiver.spring.integration.file.filters;

import com.google.common.testing.AbstractPackageSanityTests;
import fi.fmi.avi.archiver.ProcessingState;
import fi.fmi.avi.archiver.util.GeneratedClasses;

import java.time.Clock;

public class PackageSanityTest extends AbstractPackageSanityTests {
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        ignoreClasses(GeneratedClasses::isKnownGenerated);

        setDefault(ProcessingState.class, new ProcessingState(Clock.systemUTC()));
    }
}
