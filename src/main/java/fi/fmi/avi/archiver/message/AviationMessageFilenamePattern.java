package fi.fmi.avi.archiver.message;

import static java.util.Objects.requireNonNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AviationMessageFilenamePattern {

    static final String YEAR = "yyyy";
    static final String MONTH = "MM";
    static final String DAY = "dd";
    static final String HOUR = "hh";
    static final String MINUTE = "mm";
    static final String SECOND = "ss";

    private final String filename;
    private final Pattern pattern;

    public AviationMessageFilenamePattern(final String filename, final Pattern pattern) {
        this.filename = requireNonNull(filename, "filename");
        this.pattern = requireNonNull(pattern, "pattern");
    }

    public String getString(final String groupName) {
        final Matcher matcher = pattern.matcher(filename);
        if (matcher.find()) {
            return matcher.group(groupName);
        }
        throw new IllegalArgumentException("No match found");
    }

    int getInt(final String groupName) {
        final Matcher matcher = pattern.matcher(filename);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(groupName));
        }
        throw new IllegalArgumentException("No match found");
    }

}
