package fi.fmi.avi.archiver.message;

import com.google.common.testing.AbstractPackageSanityTests;

public class PackageSanityTest extends AbstractPackageSanityTests {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        setDefault(ArchiveAviationMessageIWXXMDetails.class, ArchiveAviationMessageIWXXMDetails.builder().buildPartial());
        setDefault(ArchiveAviationMessage.class, ArchiveAviationMessage.builder().buildPartial());
    }

}
