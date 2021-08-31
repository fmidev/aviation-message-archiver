package fi.fmi.avi.archiver.file;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;

public class FilenameMatcher {

    public static final String YEAR = "yyyy";
    public static final String MONTH = "MM";
    public static final String DAY = "dd";
    public static final String HOUR = "hh";
    public static final String MINUTE = "mm";
    public static final String SECOND = "ss";

    private final Matcher matcher;

    public FilenameMatcher(final String filename, final Pattern pattern) {
        requireNonNull(filename, "filename");
        requireNonNull(pattern, "pattern");

        this.matcher = pattern.matcher(filename);
        if (!matcher.find()) {
            throw new IllegalArgumentException("No match found in filename " + filename + " with pattern " + pattern);
        }
    }

    public Optional<String> getString(final String groupName) {
        return Optional.ofNullable(matcher.group(groupName));
    }

    public OptionalInt getInt(final String groupName) {
        final String group = matcher.group(groupName);
        if (group == null || group.isEmpty()) {
            return OptionalInt.empty();
        }
        try {
            return OptionalInt.of(Integer.parseInt(group));
        } catch (final NumberFormatException e) {
            return OptionalInt.empty();
        }
    }

}
