package fi.fmi.avi.archiver.spring.messaging;

import org.junit.Before;

import com.google.common.testing.AbstractPackageSanityTests;

import fi.fmi.avi.archiver.util.GeneratedClasses;

@SuppressWarnings("UnstableApiUsage")
public class PackageSanityTest extends AbstractPackageSanityTests {
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        ignoreClasses(GeneratedClasses::isKnownGenerated);
    }
}
