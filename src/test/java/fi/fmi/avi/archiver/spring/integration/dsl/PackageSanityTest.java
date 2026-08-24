package fi.fmi.avi.archiver.spring.integration.dsl;

import fi.fmi.avi.archiver.spring.messaging.MessageHeaderReference;

public class PackageSanityTest extends com.google.common.testing.AbstractPackageSanityTests {
    @Override
    public void setUp() throws Exception {
        super.setUp();

        setDefault(MessageHeaderReference.class, MessageHeaderReference.simpleNameOf(MessageHeaderReference.class));
    }
}
