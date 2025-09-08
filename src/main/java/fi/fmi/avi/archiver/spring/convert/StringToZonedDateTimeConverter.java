package fi.fmi.avi.archiver.spring.convert;

import javax.annotation.Nullable;
import java.time.ZonedDateTime;

public class StringToZonedDateTimeConverter extends AbstractNonEmptyStringConverter<ZonedDateTime> {
    @Nullable
    @Override
    public ZonedDateTime convertNonEmpty(final String source) {
        return ZonedDateTime.parse(source);
    }
}
