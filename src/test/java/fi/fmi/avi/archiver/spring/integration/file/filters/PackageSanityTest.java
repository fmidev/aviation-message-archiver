package fi.fmi.avi.archiver.spring.integration.file.filters;

import com.google.common.testing.AbstractPackageSanityTests;
import fi.fmi.avi.archiver.ProcessingState;

import java.time.Clock;

public class PackageSanityTest extends AbstractPackageSanityTests {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setDefault(ProcessingState.class, new ProcessingState(Clock.systemUTC()));
    }
}
