package fi.fmi.avi.archiver.message.populator.conditional;

import com.google.common.testing.AbstractPackageSanityTests;
import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;

@SuppressWarnings("UnstableApiUsage")
public class PackageSanityTest extends AbstractPackageSanityTests {
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        setDefault(ArchiveAviationMessage.class, ArchiveAviationMessage.builder().buildPartial());
        setDefault(ArchiveAviationMessage.Builder.class, ArchiveAviationMessage.builder());
        setDefault(GeneralPropertyPredicate.class, GeneralPropertyPredicate.builder().buildPartial());
        setDefault(GeneralPropertyPredicate.Builder.class, GeneralPropertyPredicate.builder());
        setDefault(InputAviationMessage.class, InputAviationMessage.builder().buildPartial());
    }
}
