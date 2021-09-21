package fi.fmi.avi.archiver.spring.convert;

import javax.annotation.Nullable;

import org.springframework.core.convert.converter.Converter;

public abstract class AbstractNonEmptyStringConverter<T> implements Converter<String, T> {
    @Nullable
    @Override
    public final T convert(final String source) {
        return source.isEmpty() ? getEmptyValue() : convertNonEmpty(source);
    }

    @Nullable
    protected T getEmptyValue() {
        return null;
    }

    protected abstract T convertNonEmpty(String source);
}
