package fi.fmi.avi.archiver.spring.convert;

import java.time.ZonedDateTime;

public class StringToZonedDateTimeConverter extends AbstractNonEmptyStringConverter<ZonedDateTime> {
    @Override
    public ZonedDateTime convertNonEmpty(final String source) {
        return ZonedDateTime.parse(source);
    }
}
