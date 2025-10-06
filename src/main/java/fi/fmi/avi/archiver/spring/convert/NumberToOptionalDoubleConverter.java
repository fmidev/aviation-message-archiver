package fi.fmi.avi.archiver.spring.convert;

import org.springframework.core.convert.converter.Converter;

import java.util.OptionalDouble;

public class NumberToOptionalDoubleConverter implements Converter<Number, OptionalDouble> {
    @Override
    public OptionalDouble convert(final Number source) {
        return OptionalDouble.of(source.doubleValue());
    }
}
