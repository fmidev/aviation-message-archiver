package fi.fmi.avi.archiver.message.populator.conditional;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class RenamingConditionPropertyReaderFactoryTest {
    @Mock
    private ConditionPropertyReaderFactory delegate;
    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (mocks != null) {
            mocks.close();
        }
    }

    @Test
    void getInstance_delegates_through_nameTransformer() {
        final RenamingConditionPropertyReaderFactory factory = new RenamingConditionPropertyReaderFactory(delegate, name -> name + "SPAM");

        factory.getInstance("testName");

        verify(delegate).getInstance("testNameSPAM");
    }
}
