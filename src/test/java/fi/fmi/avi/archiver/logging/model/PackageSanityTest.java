package fi.fmi.avi.archiver.logging.model;

import com.google.common.testing.AbstractPackageSanityTests;

import fi.fmi.avi.archiver.file.FileProcessingIdentifier;
import fi.fmi.avi.archiver.util.GeneratedClasses;

@SuppressWarnings("UnstableApiUsage")
public class PackageSanityTest extends AbstractPackageSanityTests {
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        ignoreClasses(GeneratedClasses::isKnownGenerated);
        ignoreClasses(FileProcessingStatisticsSpec.class::equals);

        setDefault(BulletinLogReference.class, BulletinLogReference.builder().buildPartial());
        setDefault(FileProcessingIdentifier.class, FileProcessingIdentifier.newInstance());
        setDefault(MessageLogReference.class, MessageLogReference.builder().buildPartial());
    }
}
