package fi.fmi.avi.archiver.spring.integration.dsl;

import org.junit.Before;

import fi.fmi.avi.archiver.spring.messaging.MessageHeaderReference;

@SuppressWarnings("UnstableApiUsage")
public class PackageSanityTest extends com.google.common.testing.AbstractPackageSanityTests {
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        setDefault(MessageHeaderReference.class, MessageHeaderReference.simpleNameOf(MessageHeaderReference.class));
    }
}
