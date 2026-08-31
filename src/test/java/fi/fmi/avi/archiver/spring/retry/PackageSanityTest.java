package fi.fmi.avi.archiver.spring.retry;

import com.google.common.testing.AbstractPackageSanityTests;

import fi.fmi.avi.archiver.util.GeneratedClasses;

public class PackageSanityTest extends AbstractPackageSanityTests {

    @Override
    public void setUp() throws Exception {
        super.setUp();

        ignoreClasses(GeneratedClasses::isKnownGenerated);
    }
}
