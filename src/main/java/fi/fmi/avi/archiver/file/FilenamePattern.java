package fi.fmi.avi.archiver.file;

import static java.util.Objects.requireNonNull;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import java.util.EnumMap;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fi.fmi.avi.archiver.util.TimeUtil;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;

public class FilenamePattern {
    public static final String YEAR = "yyyy";
    public static final String MONTH = "MM";
    public static final String DAY = "dd";
    public static final String HOUR = "hh";
    public static final String MINUTE = "mm";
    public static final String SECOND = "ss";
    private final String filename;
    private final Pattern pattern;
    private final ZoneId timestampZone;

    @Deprecated
    public FilenamePattern(final String filename, final Pattern pattern) {
        this(filename, pattern, ZoneOffset.UTC);
    }

    public FilenamePattern(final String filename, final Pattern pattern, final ZoneId timestampZone) {
        this.filename = requireNonNull(filename, "filename");
        this.pattern = requireNonNull(pattern, "pattern");
        this.timestampZone = requireNonNull(timestampZone, "timestampZone");
    }

    public String getString(final String groupName) {
        final Matcher matcher = pattern.matcher(filename);
        if (matcher.find()) {
            return matcher.group(groupName);
        }
        throw new IllegalArgumentException("No match found");
    }

    public Optional<String> getStringOptional(final String groupName) {
        final Matcher matcher = pattern.matcher(filename);
        if (matcher.find()) {
            try {
                return Optional.ofNullable(matcher.group(groupName));
            } catch (final RuntimeException ignored) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    public int getInt(final String groupName) {
        final Matcher matcher = pattern.matcher(filename);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(groupName));
        }
        throw new IllegalArgumentException("No match found");
    }

    public OptionalInt getIntOptional(final String groupName) {
        final Matcher matcher = pattern.matcher(filename);
        if (matcher.find()) {
            try {
                return OptionalInt.of(Integer.parseInt(matcher.group(groupName)));
            } catch (final RuntimeException ignored) {
                return OptionalInt.empty();
            }
        }
        return OptionalInt.empty();
    }

    public Optional<PartialOrCompleteTimeInstant> getTimestamp() {
        final EnumMap<ChronoField, Integer> temporalFieldValues = new EnumMap<>(ChronoField.class);
        getIntOptional(SECOND).ifPresent(second -> temporalFieldValues.put(ChronoField.SECOND_OF_MINUTE, second));
        getIntOptional(MINUTE).ifPresent(minute -> temporalFieldValues.put(ChronoField.MINUTE_OF_HOUR, minute));
        getIntOptional(HOUR).ifPresent(hour -> temporalFieldValues.put(ChronoField.HOUR_OF_DAY, hour));
        getIntOptional(DAY).ifPresent(day -> temporalFieldValues.put(ChronoField.DAY_OF_MONTH, day));
        getIntOptional(MONTH).ifPresent(month -> temporalFieldValues.put(ChronoField.MONTH_OF_YEAR, month));
        getIntOptional(YEAR).ifPresent(year -> temporalFieldValues.put(ChronoField.YEAR, year));
        return TimeUtil.toPartialOrCompleteTimeInstant(temporalFieldValues, timestampZone);
    }
}
