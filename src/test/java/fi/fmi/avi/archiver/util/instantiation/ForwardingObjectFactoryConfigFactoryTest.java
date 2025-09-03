package fi.fmi.avi.archiver.util.instantiation;

import com.google.common.testing.ForwardingWrapperTester;
import org.junit.jupiter.api.Test;

class ForwardingObjectFactoryConfigFactoryTest {
    @Test
    void testForwarding() {
        new ForwardingWrapperTester().testForwarding(ObjectFactoryConfigFactory.class, delegate -> new ForwardingObjectFactoryConfigFactory() {
            @Override
            protected ObjectFactoryConfigFactory delegate() {
                return delegate;
            }
        });
    }
}
