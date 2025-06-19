package fi.fmi.avi.archiver.message;

import com.google.common.testing.AbstractPackageSanityTests;
import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.util.GeneratedClasses;

public class PackageSanityTest extends AbstractPackageSanityTests {

    @Override
    public void setUp() throws Exception {
        super.setUp();

        ignoreClasses(GeneratedClasses::isKnownGenerated);

        setDefault(ArchiveAviationMessageIWXXMDetails.class, ArchiveAviationMessageIWXXMDetails.builder().buildPartial());
        setDefault(ArchiveAviationMessage.class, ArchiveAviationMessage.builder().buildPartial());
        setDefault(InputAviationMessage.class, InputAviationMessage.builder().buildPartial());
    }

}
