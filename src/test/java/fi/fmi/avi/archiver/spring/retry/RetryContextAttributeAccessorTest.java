package fi.fmi.avi.archiver.spring.retry;

import static fi.fmi.avi.archiver.spring.retry.RetryContextAttributeAccessor.builder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.retry.RetryContext;

class RetryContextAttributeAccessorTest {
    private static final int DEFAULT_VALUE = 17;
    private static final int ATTRIBUTE_VALUE = 23;
    private static final String ATTRIBUTE_NAME = "my.test.attribute.name";

    @Mock
    private RetryContext context;
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
    void builder_setNameFromType_uses_class_name_as_name() {
        final Class<Integer> type = Integer.class;
        final RetryContextAttributeAccessor.Builder<Integer> builder = builder(type)//
                .setNameFromType();

        assertThat(builder.getName()).isEqualTo(type.getName());
    }

    @Test
    void get_given_context_with_missing_attribute_value_returns_default_value() {
        final RetryContextAttributeAccessor<Integer> accessor = builder(Integer.class)//
                .setName(ATTRIBUTE_NAME)//
                .setDefaultValue(DEFAULT_VALUE)//
                .build();
        when(context.getAttribute(any())).thenReturn(null);
        when(context.hasAttribute(any())).thenReturn(false);

        final Integer result = accessor.get(context);

        assertThat(result).isEqualTo(DEFAULT_VALUE);
        verify(context).getAttribute(accessor.getName());
    }

    @Test
    void get_given_context_with_attribute_value_of_different_type_returns_default_value() {
        final RetryContextAttributeAccessor<Integer> accessor = builder(Integer.class)//
                .setName(ATTRIBUTE_NAME)//
                .setDefaultValue(DEFAULT_VALUE)//
                .build();
        when(context.getAttribute(ATTRIBUTE_NAME)).thenReturn("something else");

        final Integer result = accessor.get(context);

        assertThat(result).isEqualTo(DEFAULT_VALUE);
        verify(context).getAttribute(accessor.getName());
    }

    @Test
    void get_given_context_with_proper_attribute_value_returns_the_value() {
        final RetryContextAttributeAccessor<Integer> accessor = builder(Integer.class)//
                .setName(ATTRIBUTE_NAME)//
                .setDefaultValue(DEFAULT_VALUE)//
                .build();
        when(context.getAttribute(ATTRIBUTE_NAME)).thenReturn(ATTRIBUTE_VALUE);

        final Integer result = accessor.get(context);

        assertThat(result).isEqualTo(ATTRIBUTE_VALUE);
    }

    @Test
    void get_passes_attribute_value_to_doOnGet() {
        final int modifiedValue = 45;
        final AtomicReference<Integer> readValue = new AtomicReference<>();
        final RetryContextAttributeAccessor<Integer> accessor = builder(Integer.class)//
                .setName(ATTRIBUTE_NAME)//
                .setDefaultValue(DEFAULT_VALUE)//
                .setDoOnGet(value -> {
                    readValue.set(value);
                    return modifiedValue;
                })//
                .build();
        when(context.getAttribute(ATTRIBUTE_NAME)).thenReturn(ATTRIBUTE_VALUE);

        final Integer result = accessor.get(context);

        assertThat(result).isEqualTo(modifiedValue);
        assertThat(readValue).hasValue(ATTRIBUTE_VALUE);
    }

    @Test
    void set_sets_value_to_context() {
        final RetryContextAttributeAccessor<Integer> accessor = builder(Integer.class)//
                .setName(ATTRIBUTE_NAME)//
                .setDefaultValue(DEFAULT_VALUE)//
                .build();

        accessor.set(context, ATTRIBUTE_VALUE);

        verify(context).setAttribute(ATTRIBUTE_NAME, ATTRIBUTE_VALUE);
    }

    @Test
    void set_passes_attribute_value_to_doOnSet() {
        final int modifiedValue = 45;
        final AtomicReference<Integer> readValue = new AtomicReference<>();
        final RetryContextAttributeAccessor<Integer> accessor = builder(Integer.class)//
                .setName(ATTRIBUTE_NAME)//
                .setDefaultValue(DEFAULT_VALUE)//
                .setDoOnSet(value -> {
                    readValue.set(value);
                    return modifiedValue;
                })//
                .build();

        accessor.set(context, ATTRIBUTE_VALUE);

        assertThat(readValue).hasValue(ATTRIBUTE_VALUE);
        verify(context).setAttribute(ATTRIBUTE_NAME, modifiedValue);
    }

    @Test
    void clear_removes_attribute() {
        final RetryContextAttributeAccessor<Integer> accessor = builder(Integer.class)//
                .setName(ATTRIBUTE_NAME)//
                .setDefaultValue(DEFAULT_VALUE)//
                .build();

        accessor.clear(context);

        verify(context).removeAttribute(ATTRIBUTE_NAME);
    }
}
