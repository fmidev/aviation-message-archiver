package fi.fmi.avi.archiver.util.instantiation;

import com.google.common.testing.ForwardingWrapperTester;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.jupiter.api.Test;

@SuppressFBWarnings("SIC_INNER_SHOULD_BE_STATIC_ANON")
class ForwardingObjectFactoryTest {
    @SuppressWarnings("rawtypes")
    @Test
    void testForwarding() {
        new ForwardingWrapperTester().testForwarding(ObjectFactory.class, delegate -> new ForwardingObjectFactory() {
            @Override
            protected ObjectFactory delegate() {
                return delegate;
            }
        });
    }
}
