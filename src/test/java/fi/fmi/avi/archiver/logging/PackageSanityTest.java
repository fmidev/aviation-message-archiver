package fi.fmi.avi.archiver.logging;

import com.google.common.testing.AbstractPackageSanityTests;

import fi.fmi.avi.archiver.file.FileProcessingIdentifier;

@SuppressWarnings("UnstableApiUsage")
public class PackageSanityTest extends AbstractPackageSanityTests {
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        setDefault(BulletinLogReference.class, BulletinLogReference.builder().buildPartial());
        setDefault(FileProcessingIdentifier.class, FileProcessingIdentifier.newInstance());
        setDefault(MessageLogReference.class, MessageLogReference.builder().buildPartial());
    }
}
