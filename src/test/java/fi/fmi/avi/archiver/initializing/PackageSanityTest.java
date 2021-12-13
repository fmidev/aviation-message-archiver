package fi.fmi.avi.archiver.initializing;

import com.google.common.testing.AbstractPackageSanityTests;
import fi.fmi.avi.archiver.ProcessingState;

import java.time.Clock;

import static org.mockito.Mockito.mock;

public class PackageSanityTest extends AbstractPackageSanityTests {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        setDefault(Clock.class, Clock.systemUTC());
        setDefault(ProcessingState.class, mock(ProcessingState.class));
    }

}
