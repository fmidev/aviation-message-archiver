package fi.fmi.avi.archiver.util;

import java.time.ZoneId;
import java.time.ZoneOffset;

import com.google.common.testing.AbstractPackageSanityTests;

import fi.fmi.avi.model.PartialDateTime;

@SuppressWarnings("UnstableApiUsage")
public class PackageSanityTest extends AbstractPackageSanityTests {
    @Override
    public void setUp() throws Exception {
        super.setUp();

        ignoreClasses(GeneratedClasses::isKnownGeneratedClass);

        setDefault(PartialDateTime.class, PartialDateTime.ofHour(0));
        setDefault(ZoneId.class, ZoneOffset.UTC);
    }
}
