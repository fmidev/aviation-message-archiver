package fi.fmi.avi.archiver.spring.convert;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class EmptyStringToEmptyMapConverterTest {
    private EmptyStringToEmptyMapConverter converter;

    @BeforeEach
    void setUp() {
        converter = new EmptyStringToEmptyMapConverter();
    }

    @Test
    void converts_empty_string_to_empty_map() {
        assertThat(converter.convert("")).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            " ",
            "\n",
            "a",
            "a=b",
            "a=b,b=c",
    })
    void fails_on_non_empty_string(final String input) {
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> System.out.println(converter.convert(input)));
    }
}
