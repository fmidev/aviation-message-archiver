package fi.fmi.avi.archiver.message.processor;

import com.google.common.testing.AbstractPackageSanityTests;
import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.util.GeneratedClasses;

@SuppressWarnings("UnstableApiUsage")
public class PackageSanityTest extends AbstractPackageSanityTests {

    @Override
    public void setUp() throws Exception {
        super.setUp();

        ignoreClasses(GeneratedClasses::isKnownGenerated);

        setDefault(InputAviationMessage.class, InputAviationMessage.builder().buildPartial());
    }

}
