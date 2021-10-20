package fi.fmi.avi.archiver.spring.convert;

import java.util.regex.Pattern;

public class StringToPatternConverter extends AbstractNonEmptyStringConverter<Pattern> {
    @Override
    public Pattern convertNonEmpty(final String source) {
        return Pattern.compile(source);
    }
}
