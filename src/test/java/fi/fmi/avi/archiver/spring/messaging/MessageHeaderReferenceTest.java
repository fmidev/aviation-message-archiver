package fi.fmi.avi.archiver.spring.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.when;

import java.util.Optional;

import javax.annotation.Nullable;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.messaging.MessageHeaders;

class MessageHeaderReferenceTest {
    private static final String NAME = "testHeaderName";
    private static final Class<Integer> TYPE = Integer.class;
    private static final Integer VALUE = 5;

    @Mock
    private MessageHeaders headers;
    private AutoCloseable mocks;

    private MessageHeaderReference<Integer> headerRef;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        headerRef = MessageHeaderReference.of(NAME, TYPE);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (mocks != null) {
            mocks.close();
        }
    }

    @Test
    void of_given_name_and_type_constructs_instance() {
        headerRef = MessageHeaderReference.of(NAME, TYPE);

        assertSoftly(softly -> {
            softly.assertThat(headerRef.getName()).as("name").isEqualTo(NAME);
            softly.assertThat(headerRef.getType()).as("type").isEqualTo(TYPE);
        });
    }

    @Test
    void simpleNameOf_given_type_constructs_instance_with_type_simple_name_as_name() {
        final Class<MessageHeaderReferenceTest> type = MessageHeaderReferenceTest.class;

        final MessageHeaderReference<MessageHeaderReferenceTest> headerRef = MessageHeaderReference.simpleNameOf(type);

        assertSoftly(softly -> {
            softly.assertThat(headerRef.getName()).as("name").isEqualTo(type.getSimpleName());
            softly.assertThat(headerRef.getType()).as("type").isEqualTo(type);
        });
    }

    @Test
    void toString_returns_name() {
        assertThat(headerRef.toString()).isEqualTo(headerRef.getName());
    }

    @Test
    void getNonNull_returns_null_if_header_is_null() {
        when(headers.get(NAME, TYPE)).thenReturn(null);

        assertThatNullPointerException()//
                .isThrownBy(() -> headerRef.getNonNull(headers))//
                .withMessageContaining(headerRef.getName());
    }

    @Test
    void getNonNull_returns_value_when_not_null() {
        when(headers.get(NAME, TYPE)).thenReturn(VALUE);

        final Integer result = headerRef.getNonNull(headers);

        assertThat(result).isEqualTo(VALUE);
    }

    @Test
    void getNullable_returns_null_if_header_is_null() {
        when(headers.get(NAME, TYPE)).thenReturn(null);

        @Nullable
        final Integer result = headerRef.getNullable(headers);

        assertThat(result).isNull();
    }

    @Test
    void getNullable_returns_value_when_not_null() {
        when(headers.get(NAME, TYPE)).thenReturn(VALUE);

        @Nullable
        final Integer result = headerRef.getNullable(headers);

        assertThat(result).isEqualTo(VALUE);
    }

    @Test
    void getOptional_returns_empty_if_header_is_null() {
        when(headers.get(NAME, TYPE)).thenReturn(null);

        final Optional<Integer> result = headerRef.getOptional(headers);

        assertThat(result).isEmpty();
    }

    @Test
    void getOptional_returns_value_when_not_null() {
        when(headers.get(NAME, TYPE)).thenReturn(VALUE);

        final Optional<Integer> result = headerRef.getOptional(headers);

        assertThat(result).hasValue(VALUE);
    }
}
