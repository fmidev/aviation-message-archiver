package fi.fmi.avi.archiver.spring.convert;

import java.time.Duration;

public class StringToDurationConverter implements NonEmptyStringConverter<Duration> {
    @Override
    public Duration convertNonEmpty(final String source) {
        return Duration.parse(source);
    }
}
