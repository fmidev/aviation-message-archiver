package fi.fmi.avi.archiver.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings("UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR")
class GenericStructuredLoggableTest {
    private static final String NAME = "testStructureName";
    private static final String STRING = "log message";
    private static final int VALUE_INVOCATIONS = 2;
    private static final int STRING_INVOCTIONS = 3;

    @Mock
    private Supplier<Object> valueSupplier;
    @Mock
    private Supplier<String> stringSupplier;
    private AutoCloseable mocks;
    private Object value;

    private static GenericStructuredLoggable<Object> lazyLoggable(final String name, final Supplier<Object> valueSupplier, final Supplier<String> stringSupplier) {
        return GenericStructuredLoggable.loggable(name, valueSupplier, stringSupplier);
    }

    private static GenericStructuredLoggable<Object> immutableLoggable(final String name, final Object value, final String string) {
        return GenericStructuredLoggable.loggable(name, value, string);
    }

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);

        value = new Object();
        when(valueSupplier.get()).thenReturn(value);
        when(stringSupplier.get()).thenReturn(STRING);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (mocks != null) {
            mocks.close();
        }
    }

    <T> void assertLazyLoggable(final GenericStructuredLoggable<T> loggable, @Nullable final T value, final String string) {
        assertSoftly(softly -> {
            softly.assertThat(loggable.getStructureName()).as("getStructureName").isEqualTo(NAME);
            softly.assertThat(loggable.estimateLogStringLength()).as("estimateLogStringLength").isEqualTo(0);
            softly.assertThat(loggable.getValue()).as("getValue").isEqualTo(value);
            softly.assertThat(loggable.toString()).as("toString").isEqualTo(string);
        });
        assertThat(loggable.getValue()).as("getValue 2").isEqualTo(value);
        assertThat(loggable.toString()).as("toString 2").isEqualTo(string);
        assertThat(loggable.toString()).as("toString 3").isEqualTo(string);
    }

    <T> void assertImmutableLoggable(final GenericStructuredLoggable<T> loggable, @Nullable final T value, final String string) {
        assertSoftly(softly -> {
            softly.assertThat(loggable.getStructureName()).as("getStructureName").isEqualTo(NAME);
            softly.assertThat(loggable.getValue()).as("getValue").isEqualTo(value);
            softly.assertThat(loggable.getValue()).as("getValue same").isSameAs(loggable.getValue());
            softly.assertThat(loggable.toString()).as("toString").isEqualTo(string);
            softly.assertThat(loggable.toString()).as("toString same").isSameAs(loggable.toString());
            softly.assertThat(loggable.estimateLogStringLength()).as("estimateLogStringLength").isEqualTo(string.length());
        });
        verifyNoInteractions(valueSupplier, stringSupplier);
    }

    @Test
    void structured_String_Supplier_Supplier_returns_lazy_instance() {
        final GenericStructuredLoggable<Object> loggable = GenericStructuredLoggable.loggable(NAME, valueSupplier, stringSupplier);

        assertLazyLoggable(loggable, value, STRING);
        verify(valueSupplier, times(VALUE_INVOCATIONS)).get();
        verify(stringSupplier, times(STRING_INVOCTIONS)).get();
    }

    @Test
    void structured_String_Supplier_Supplier_accepts_valueSupplier_returning_null() {
        when(valueSupplier.get()).thenReturn(null);

        final GenericStructuredLoggable<Object> loggable = GenericStructuredLoggable.loggable(NAME, valueSupplier, stringSupplier);

        assertLazyLoggable(loggable, null, STRING);
        verify(valueSupplier, times(VALUE_INVOCATIONS)).get();
        verify(stringSupplier, times(STRING_INVOCTIONS)).get();
    }

    @Test
    void structured_String_Supplier_Supplier_accepts_stringSupplier_returning_null() {
        when(stringSupplier.get()).thenReturn(null);

        final GenericStructuredLoggable<Object> loggable = GenericStructuredLoggable.loggable(NAME, valueSupplier, stringSupplier);

        assertLazyLoggable(loggable, value, "null");
        verify(valueSupplier, times(VALUE_INVOCATIONS)).get();
        verify(stringSupplier, times(STRING_INVOCTIONS)).get();
    }

    @Test
    void structured_String_T_String_returns_immutable_instance() {
        final GenericStructuredLoggable<Object> loggable = GenericStructuredLoggable.loggable(NAME, value, STRING);

        assertImmutableLoggable(loggable, value, STRING);
    }

    @Test
    void structured_String_T_String_accepts_null_value() {
        final GenericStructuredLoggable<Object> loggable = GenericStructuredLoggable.loggable(NAME, null, STRING);

        assertImmutableLoggable(loggable, null, STRING);
    }

    @Test
    void structuredString_String_Supplier_returns_lazy_instance() {
        final GenericStructuredLoggable<String> loggable = GenericStructuredLoggable.loggableString(NAME, stringSupplier);

        assertLazyLoggable(loggable, STRING, STRING);
        verify(valueSupplier, never()).get();
        verify(stringSupplier, times(VALUE_INVOCATIONS + STRING_INVOCTIONS)).get();
    }

    @Test
    void structuredString_String_Supplier_accepts_stringSupplier_returning_null() {
        when(stringSupplier.get()).thenReturn(null);
        final GenericStructuredLoggable<String> loggable = GenericStructuredLoggable.loggableString(NAME, stringSupplier);

        assertLazyLoggable(loggable, null, "null");
        verify(valueSupplier, never()).get();
        verify(stringSupplier, times(VALUE_INVOCATIONS + STRING_INVOCTIONS)).get();
    }

    @Test
    void structuredString_String_String_returns_immutable_instance() {
        final GenericStructuredLoggable<String> loggable = GenericStructuredLoggable.loggableString(NAME, STRING);

        assertImmutableLoggable(loggable, STRING, STRING);
    }

    @Test
    void structuredValue_String_Supplier_returns_lazy_instance() {
        final GenericStructuredLoggable<Object> loggable = GenericStructuredLoggable.loggableValue(NAME, valueSupplier);

        assertLazyLoggable(loggable, value, value.toString());
        verify(valueSupplier, times(VALUE_INVOCATIONS + STRING_INVOCTIONS)).get();
        verify(stringSupplier, never()).get();
    }

    @Test
    void structuredValue_String_Supplier_accepts_valueSupplier_returning_null() {
        when(valueSupplier.get()).thenReturn(null);

        final GenericStructuredLoggable<Object> loggable = GenericStructuredLoggable.loggableValue(NAME, valueSupplier);

        assertLazyLoggable(loggable, null, "null");
        verify(valueSupplier, times(VALUE_INVOCATIONS + STRING_INVOCTIONS)).get();
        verify(stringSupplier, never()).get();
    }

    @Test
    void structuredValue_String_T_returns_lazy_instance() {
        final GenericStructuredLoggable<Object> loggable = GenericStructuredLoggable.loggableValue(NAME, value);

        assertLazyLoggable(loggable, value, value.toString());
        verify(valueSupplier, never()).get();
        verify(stringSupplier, never()).get();
    }

    @Test
    void structuredValue_String_T_accepts_null_value() {
        final GenericStructuredLoggable<Object> loggable = GenericStructuredLoggable.loggableValue(NAME, (Object) null);

        assertLazyLoggable(loggable, null, "null");
        verify(valueSupplier, never()).get();
        verify(stringSupplier, never()).get();
    }

    @Nested
    class LazyValueTest {
        @Test
        void readableCopy_returns_immutable_copy() {
            final GenericStructuredLoggable<Object> loggable = lazyLoggable(NAME, valueSupplier, stringSupplier);

            final GenericStructuredLoggable<Object> copy = loggable.readableCopy();

            verify(valueSupplier).get();
            verify(stringSupplier).get();
            //noinspection unchecked
            clearInvocations(valueSupplier, stringSupplier);

            assertImmutableLoggable(copy, value, STRING);
        }
    }

    @Nested
    class ImmutableValueTest {
        @Test
        void readableCopy_returns_self() {
            final GenericStructuredLoggable<Object> loggable = immutableLoggable(NAME, value, STRING);

            final GenericStructuredLoggable<Object> copy = loggable.readableCopy();

            assertThat(copy).isSameAs(loggable);
        }
    }
}
