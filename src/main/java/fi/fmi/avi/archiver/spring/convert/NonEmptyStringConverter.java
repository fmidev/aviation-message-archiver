package fi.fmi.avi.archiver.spring.convert;

import javax.annotation.Nullable;

import org.springframework.core.convert.converter.Converter;

@FunctionalInterface
public interface NonEmptyStringConverter<T> extends Converter<String, T> {
    @Nullable
    @Override
    default T convert(final String source) {
        return source.isEmpty() ? null : convertNonEmpty(source);
    }

    T convertNonEmpty(String source);
}
