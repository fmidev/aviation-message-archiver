package fi.fmi.avi.archiver.file;

import fi.fmi.avi.archiver.util.TimeUtil;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.EnumMap;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;

public class FilenameMatcher {

    public static final String YEAR = "yyyy";
    public static final String TWO_DIGIT_YEAR = "yy";
    public static final String MONTH = "MM";
    public static final String DAY = "dd";
    public static final String HOUR = "hh";
    public static final String MINUTE = "mm";
    public static final String SECOND = "ss";

    private static final int BASE_YEAR_OFFSET = 98;

    private final Matcher matcher;
    private final ZoneId timestampZone;

    public FilenameMatcher(final String filename, final Pattern pattern, final ZoneId timestampZone) {
        requireNonNull(filename, "filename");
        requireNonNull(pattern, "pattern");
        this.timestampZone = requireNonNull(timestampZone, "timestampZone");

        this.matcher = pattern.matcher(filename);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("No match found in filename " + filename + " with pattern " + pattern);
        }
    }

    public Optional<String> getString(final String groupName) {
        requireNonNull(groupName, "groupName");
        try {
            return Optional.ofNullable(matcher.group(groupName));
        } catch (final RuntimeException ignored) {
            return Optional.empty();
        }
    }

    public OptionalInt getInt(final String groupName) {
        requireNonNull(groupName, "groupName");
        try {
            return OptionalInt.of(Integer.parseInt(matcher.group(groupName)));
        } catch (final RuntimeException ignored) {
            return OptionalInt.empty();
        }
    }

    public Optional<PartialOrCompleteTimeInstant> getTimestamp(final Clock clock) {
        requireNonNull(clock, "clock");
        final EnumMap<ChronoField, Integer> temporalFieldValues = new EnumMap<>(ChronoField.class);
        getInt(SECOND).ifPresent(second -> temporalFieldValues.put(ChronoField.SECOND_OF_MINUTE, second));
        getInt(MINUTE).ifPresent(minute -> temporalFieldValues.put(ChronoField.MINUTE_OF_HOUR, minute));
        getInt(HOUR).ifPresent(hour -> temporalFieldValues.put(ChronoField.HOUR_OF_DAY, hour));
        getInt(DAY).ifPresent(day -> temporalFieldValues.put(ChronoField.DAY_OF_MONTH, day));
        getInt(MONTH).ifPresent(month -> temporalFieldValues.put(ChronoField.MONTH_OF_YEAR, month));

        final OptionalInt fullYear = getInt(YEAR);
        if (fullYear.isPresent()) {
            temporalFieldValues.put(ChronoField.YEAR, fullYear.getAsInt());
        } else {
            getString(TWO_DIGIT_YEAR).ifPresent(twoDigitYear -> {
                final TemporalAccessor parsedYear = new DateTimeFormatterBuilder()
                        // The next year is the furthest year we allow
                        .appendValueReduced(ChronoField.YEAR, 2, 2, LocalDate.now(clock)
                                .minusYears(BASE_YEAR_OFFSET)).toFormatter()
                        .parse(twoDigitYear);
                temporalFieldValues.put(ChronoField.YEAR, parsedYear.get(ChronoField.YEAR));
            });
        }

        return TimeUtil.toPartialOrCompleteTimeInstant(temporalFieldValues, timestampZone);
    }

}
