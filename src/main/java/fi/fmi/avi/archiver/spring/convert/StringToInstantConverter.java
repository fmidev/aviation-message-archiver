package fi.fmi.avi.archiver.spring.convert;

import java.time.Instant;

import javax.annotation.Nullable;

public class StringToInstantConverter extends AbstractNonEmptyStringConverter<Instant> {
    @Nullable
    @Override
    public Instant convertNonEmpty(final String source) {
        return Instant.parse(source);
    }
}
