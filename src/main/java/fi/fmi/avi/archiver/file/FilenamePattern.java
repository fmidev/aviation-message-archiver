package fi.fmi.avi.archiver.file;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;

public class FilenamePattern {

    public static final String YEAR = "yyyy";
    public static final String MONTH = "MM";
    public static final String DAY = "dd";
    public static final String HOUR = "hh";
    public static final String MINUTE = "mm";
    public static final String SECOND = "ss";

    private final String filename;
    private final Pattern pattern;

    public FilenamePattern(final String filename, final Pattern pattern) {
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

    public int getInt(final String groupName) {
        final Matcher matcher = pattern.matcher(filename);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(groupName));
        }
        throw new IllegalArgumentException("No match found");
    }

}
