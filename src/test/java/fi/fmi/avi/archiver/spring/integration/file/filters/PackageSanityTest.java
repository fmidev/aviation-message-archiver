package fi.fmi.avi.archiver.spring.integration.file.filters;

import java.time.Clock;

import com.google.common.testing.AbstractPackageSanityTests;

import fi.fmi.avi.archiver.ProcessingState;
import fi.fmi.avi.archiver.util.GeneratedClasses;

@SuppressWarnings("UnstableApiUsage")
public class PackageSanityTest extends AbstractPackageSanityTests {
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        ignoreClasses(GeneratedClasses::isKnownGenerated);

        setDefault(ProcessingState.class, new ProcessingState(Clock.systemUTC()));
    }
}
