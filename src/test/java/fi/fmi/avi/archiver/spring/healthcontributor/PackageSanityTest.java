package fi.fmi.avi.archiver.spring.healthcontributor;

import com.google.common.testing.AbstractPackageSanityTests;
import fi.fmi.avi.archiver.spring.integration.util.MonitorableCallerBlocksPolicy;

import static org.mockito.Mockito.mock;

public class PackageSanityTest extends AbstractPackageSanityTests {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        setDefault(MonitorableCallerBlocksPolicy.class, mock(MonitorableCallerBlocksPolicy.class));
    }

}
