package fi.fmi.avi.archiver.initializing;

import static org.mockito.Mockito.mock;

import com.google.common.testing.AbstractPackageSanityTests;

import fi.fmi.avi.archiver.ProcessingMetrics;

public class PackageSanityTest extends AbstractPackageSanityTests {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        setDefault(AviationProductsHolder.AviationProduct.class, AviationProductsHolder.AviationProduct.builder().buildPartial());
        setDefault(AviationProductsHolder.class, mock(AviationProductsHolder.class));
        setDefault(ProcessingMetrics.class, mock(ProcessingMetrics.class));
    }

}
