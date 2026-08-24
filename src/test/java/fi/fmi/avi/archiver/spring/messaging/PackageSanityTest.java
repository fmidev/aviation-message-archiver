package fi.fmi.avi.archiver.spring.messaging;

import com.google.common.testing.AbstractPackageSanityTests;
import fi.fmi.avi.archiver.util.GeneratedClasses;

public class PackageSanityTest extends AbstractPackageSanityTests {
    @Override
    public void setUp() throws Exception {
        super.setUp();

        ignoreClasses(GeneratedClasses::isKnownGenerated);
    }
}
