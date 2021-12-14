package fi.fmi.avi.archiver.spring.healthcontributor;

import static org.mockito.Mockito.mock;

import com.google.common.testing.AbstractPackageSanityTests;

import fi.fmi.avi.archiver.config.AviationProductConfig;
import fi.fmi.avi.archiver.spring.integration.util.MonitorableCallerBlocksPolicy;

public class PackageSanityTest extends AbstractPackageSanityTests {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        setDefault(AviationProductConfig.class, mock(AviationProductConfig.class));
        setDefault(MonitorableCallerBlocksPolicy.class, mock(MonitorableCallerBlocksPolicy.class));
    }

}
