package fi.fmi.avi.archiver.spring.convert;

import org.jspecify.annotations.Nullable;
import org.springframework.core.convert.converter.Converter;

public abstract class AbstractNonEmptyStringConverter<T> implements Converter<String, T> {
    @Override
    public final @Nullable T convert(final String source) {
        return source.isEmpty() ? getEmptyValue() : convertNonEmpty(source);
    }

    protected @Nullable T getEmptyValue() {
        return null;
    }

    protected abstract T convertNonEmpty(String source);
}
