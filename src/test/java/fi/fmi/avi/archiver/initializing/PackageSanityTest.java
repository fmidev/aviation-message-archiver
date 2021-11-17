package fi.fmi.avi.archiver.initializing;

import com.google.common.testing.AbstractPackageSanityTests;

import java.time.Clock;

import static org.mockito.Mockito.mock;

public class PackageSanityTest extends AbstractPackageSanityTests {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        setDefault(AviationProductsHolder.AviationProduct.class, AviationProductsHolder.AviationProduct.builder().buildPartial());
        setDefault(AviationProductsHolder.class, mock(AviationProductsHolder.class));
        setDefault(Clock.class, Clock.systemUTC());
    }

}
