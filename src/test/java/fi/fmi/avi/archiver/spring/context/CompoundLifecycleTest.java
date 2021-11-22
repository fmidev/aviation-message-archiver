package fi.fmi.avi.archiver.spring.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.Lifecycle;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings("UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR")
class CompoundLifecycleTest {
    @Mock
    private Lifecycle lifecycle1;
    @Mock
    private Lifecycle lifecycle2;

    private AutoCloseable mocks;
    private CompoundLifecycle compoundLifecycle;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);

        compoundLifecycle = new CompoundLifecycle();
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    @Test
    void start_delegates_to_all_added_lifecycles() {
        compoundLifecycle.add(lifecycle1);
        compoundLifecycle.add(lifecycle2);

        compoundLifecycle.start();

        verify(lifecycle1).start();
        verify(lifecycle2).start();
    }

    @Test
    void start_prevents_circular_execution() {
        final CompoundLifecycle compoundLifecycleSpy = spy(this.compoundLifecycle);
        final CompoundLifecycle circularLifecycleSpy = spy(new CompoundLifecycle());
        compoundLifecycleSpy.add(circularLifecycleSpy);
        compoundLifecycleSpy.add(lifecycle1);
        compoundLifecycleSpy.add(lifecycle2);
        circularLifecycleSpy.add(compoundLifecycleSpy);
        circularLifecycleSpy.add(lifecycle2);

        compoundLifecycleSpy.start();

        final InOrder inOrder = inOrder(compoundLifecycleSpy, circularLifecycleSpy, lifecycle1, lifecycle2);
        inOrder.verify(compoundLifecycleSpy).start();
        inOrder.verify(circularLifecycleSpy).start();
        inOrder.verify(compoundLifecycleSpy).start(); // breaking circularity
        inOrder.verify(lifecycle2).start();
        inOrder.verify(lifecycle1).start();
        inOrder.verify(lifecycle2).start();

        verify(compoundLifecycleSpy, times(2)).start();
        verify(circularLifecycleSpy, times(1)).start();
        verify(lifecycle1, times(1)).start();
        verify(lifecycle2, times(2)).start();
    }

    @Test
    void stop_delegates_to_all_added_lifecycles() {
        compoundLifecycle.add(lifecycle1);
        compoundLifecycle.add(lifecycle2);

        compoundLifecycle.stop();

        verify(lifecycle1).stop();
        verify(lifecycle2).stop();
    }

    @Test
    void stop_prevents_circular_execution() {
        final CompoundLifecycle compoundLifecycleSpy = spy(this.compoundLifecycle);
        final CompoundLifecycle circularLifecycleSpy = spy(new CompoundLifecycle());
        compoundLifecycleSpy.add(circularLifecycleSpy);
        compoundLifecycleSpy.add(lifecycle1);
        compoundLifecycleSpy.add(lifecycle2);
        circularLifecycleSpy.add(compoundLifecycleSpy);
        circularLifecycleSpy.add(lifecycle2);

        compoundLifecycleSpy.stop();

        final InOrder inOrder = inOrder(compoundLifecycleSpy, circularLifecycleSpy, lifecycle1, lifecycle2);
        inOrder.verify(compoundLifecycleSpy).stop();
        inOrder.verify(circularLifecycleSpy).stop();
        inOrder.verify(compoundLifecycleSpy).stop(); // breaking circularity
        inOrder.verify(lifecycle2).stop();
        inOrder.verify(lifecycle1).stop();
        inOrder.verify(lifecycle2).stop();

        verify(compoundLifecycleSpy, times(2)).stop();
        verify(circularLifecycleSpy, times(1)).stop();
        verify(lifecycle1, times(1)).stop();
        verify(lifecycle2, times(2)).stop();
    }

    @CsvSource({ //
            "false, false, false", //
            "true,  false, false", //
            "false, true,  false", //
            "true,  true,  true", //
    })
    @ParameterizedTest
    void isRunning_returns_true_when_all_delegates_return_true(final boolean lifecycle1Result, final boolean lifecycle2Result, final boolean expectedResult) {
        when(lifecycle1.isRunning()).thenReturn(lifecycle1Result);
        when(lifecycle2.isRunning()).thenReturn(lifecycle2Result);
        compoundLifecycle.add(lifecycle1);
        compoundLifecycle.add(lifecycle2);

        final boolean result = compoundLifecycle.isRunning();

        assertThat(result).isEqualTo(expectedResult);
    }

    @Test
    void isRunning_prevents_circular_execution() {
        when(lifecycle1.isRunning()).thenReturn(true);
        when(lifecycle2.isRunning()).thenReturn(true);
        final CompoundLifecycle compoundLifecycleSpy = spy(this.compoundLifecycle);
        final CompoundLifecycle circularLifecycleSpy = spy(new CompoundLifecycle());
        compoundLifecycleSpy.add(circularLifecycleSpy);
        compoundLifecycleSpy.add(lifecycle1);
        compoundLifecycleSpy.add(lifecycle2);
        circularLifecycleSpy.add(compoundLifecycleSpy);
        circularLifecycleSpy.add(lifecycle2);

        final boolean result = compoundLifecycleSpy.isRunning();

        assertThat(result).isTrue();
        final InOrder inOrder = inOrder(compoundLifecycleSpy, circularLifecycleSpy, lifecycle1, lifecycle2);
        inOrder.verify(compoundLifecycleSpy).isRunning();
        inOrder.verify(circularLifecycleSpy).isRunning();
        inOrder.verify(compoundLifecycleSpy).isRunning(); // breaking circularity
        inOrder.verify(lifecycle2).isRunning();
        inOrder.verify(lifecycle1).isRunning();
        inOrder.verify(lifecycle2).isRunning();

        verify(compoundLifecycleSpy, times(2)).isRunning();
        verify(circularLifecycleSpy, times(1)).isRunning();
        verify(lifecycle1, times(1)).isRunning();
        verify(lifecycle2, times(2)).isRunning();
    }

    @CsvSource({ //
            "false, false, false", //
            "true,  false, false", //
            "false, true,  false", //
            "true,  true,  true", //
    })
    @ParameterizedTest
    void isAllRunning_returns_true_when_all_delegates_return_true(final boolean lifecycle1Result, final boolean lifecycle2Result,
            final boolean expectedResult) {
        when(lifecycle1.isRunning()).thenReturn(lifecycle1Result);
        when(lifecycle2.isRunning()).thenReturn(lifecycle2Result);
        compoundLifecycle.add(lifecycle1);
        compoundLifecycle.add(lifecycle2);

        final boolean result = compoundLifecycle.isAllRunning();

        assertThat(result).isEqualTo(expectedResult);
    }

    @Test
    void isAllRunning_prevents_circular_execution() {
        when(lifecycle1.isRunning()).thenReturn(true);
        when(lifecycle2.isRunning()).thenReturn(true);
        final CompoundLifecycle compoundLifecycleSpy = spy(this.compoundLifecycle);
        final CompoundLifecycle circularLifecycleSpy = spy(new CompoundLifecycle());
        compoundLifecycleSpy.add(circularLifecycleSpy);
        compoundLifecycleSpy.add(lifecycle1);
        compoundLifecycleSpy.add(lifecycle2);
        circularLifecycleSpy.add(compoundLifecycleSpy);
        circularLifecycleSpy.add(lifecycle2);

        final boolean result = compoundLifecycleSpy.isAllRunning();

        assertThat(result).isTrue();
        final InOrder inOrder = inOrder(compoundLifecycleSpy, circularLifecycleSpy, lifecycle1, lifecycle2);
        inOrder.verify(compoundLifecycleSpy).isAllRunning();
        inOrder.verify(circularLifecycleSpy).isRunning();
        inOrder.verify(compoundLifecycleSpy).isRunning(); // breaking circularity
        inOrder.verify(lifecycle2).isRunning();
        inOrder.verify(lifecycle1).isRunning();
        inOrder.verify(lifecycle2).isRunning();

        verify(compoundLifecycleSpy, times(2)).isAllRunning();
        verify(compoundLifecycleSpy, times(1)).isRunning();
        verify(circularLifecycleSpy, times(1)).isRunning();
        verify(lifecycle1, times(1)).isRunning();
        verify(lifecycle2, times(2)).isRunning();
    }

    @CsvSource({ //
            "false, false, false", //
            "true,  false, true", //
            "false, true,  true", //
            "true,  true,  true", //
    })
    @ParameterizedTest
    void isAnyRunning_returns_true_when_any_delegate_return_true(final boolean lifecycle1Result, final boolean lifecycle2Result, final boolean expectedResult) {
        when(lifecycle1.isRunning()).thenReturn(lifecycle1Result);
        when(lifecycle2.isRunning()).thenReturn(lifecycle2Result);
        compoundLifecycle.add(lifecycle1);
        compoundLifecycle.add(lifecycle2);

        final boolean result = compoundLifecycle.isAnyRunning();

        assertThat(result).isEqualTo(expectedResult);
    }

    @Test
    void isAnyRunning_prevents_circular_execution_when_delegates_return_false() {
        when(lifecycle1.isRunning()).thenReturn(false);
        when(lifecycle2.isRunning()).thenReturn(false);
        final CompoundLifecycle compoundLifecycleSpy = spy(this.compoundLifecycle);
        final CompoundLifecycle circularLifecycleSpy = spy(new CompoundLifecycle());
        compoundLifecycleSpy.add(circularLifecycleSpy);
        compoundLifecycleSpy.add(lifecycle1);
        compoundLifecycleSpy.add(lifecycle2);
        circularLifecycleSpy.add(compoundLifecycleSpy);
        circularLifecycleSpy.add(lifecycle2);

        final boolean result = compoundLifecycleSpy.isAnyRunning();

        assertThat(result).isFalse();
        final InOrder inOrder = inOrder(compoundLifecycleSpy, circularLifecycleSpy, lifecycle1, lifecycle2);
        inOrder.verify(compoundLifecycleSpy).isAnyRunning();
        inOrder.verify(circularLifecycleSpy).isRunning();
        inOrder.verify(compoundLifecycleSpy).isRunning(); // breaking circularity
        inOrder.verify(lifecycle2).isRunning();
        inOrder.verify(lifecycle1).isRunning();
        inOrder.verify(lifecycle2).isRunning();

        verify(compoundLifecycleSpy, times(1)).isAnyRunning();
        verify(compoundLifecycleSpy, times(1)).isRunning();
        verify(circularLifecycleSpy, times(1)).isRunning();
        verify(lifecycle1, times(1)).isRunning();
        verify(lifecycle2, times(2)).isRunning();
    }

    @Test
    void isAnyRunning_prevents_circular_execution_when_delegates_return_true() {
        when(lifecycle1.isRunning()).thenReturn(true);
        when(lifecycle2.isRunning()).thenReturn(true);
        final CompoundLifecycle compoundLifecycleSpy = spy(this.compoundLifecycle);
        final CompoundLifecycle circularLifecycleSpy = spy(new CompoundLifecycle());
        compoundLifecycleSpy.add(circularLifecycleSpy);
        compoundLifecycleSpy.add(lifecycle1);
        compoundLifecycleSpy.add(lifecycle2);
        circularLifecycleSpy.add(compoundLifecycleSpy);
        circularLifecycleSpy.add(lifecycle2);

        final boolean result = compoundLifecycleSpy.isAnyRunning();

        assertThat(result).isTrue();
        final InOrder inOrder = inOrder(compoundLifecycleSpy, circularLifecycleSpy, lifecycle1, lifecycle2);
        inOrder.verify(compoundLifecycleSpy).isAnyRunning();
        inOrder.verify(circularLifecycleSpy).isRunning();
        inOrder.verify(compoundLifecycleSpy).isRunning(); // breaking circularity
        inOrder.verify(lifecycle2).isRunning();

        verify(compoundLifecycleSpy, times(1)).isAnyRunning();
        verify(compoundLifecycleSpy, times(1)).isRunning();
        verify(circularLifecycleSpy, times(1)).isRunning();
        verify(lifecycle1, times(0)).isRunning();
        verify(lifecycle2, times(1)).isRunning();
    }

}
