package fi.fmi.avi.archiver.spring.context;

import java.time.Clock;

import com.google.common.testing.AbstractPackageSanityTests;

public class PackageSanityTest extends AbstractPackageSanityTests {
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        setDefault(Clock.class, Clock.systemUTC());
    }
}
