package fi.fmi.avi.archiver.file;

import static java.util.Objects.requireNonNull;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.EnumMap;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fi.fmi.avi.archiver.util.TimeUtil;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;

/**
 * A utility to read named-capturing groups of specified {@link Pattern} matched on provided file name.
 */
public class FilenameMatcher {

    /**
     * Capturing group name for four-digit year.
     */
    public static final String YEAR = "yyyy";
    /**
     * Capturing group name for two-digit year.
     */
    public static final String TWO_DIGIT_YEAR = "yy";
    /**
     * Capturing group name for month of year.
     */
    public static final String MONTH = "MM";
    /**
     * Capturing group name for day of month.
     */
    public static final String DAY = "dd";
    /**
     * Capturing group name for hour of day.
     */
    public static final String HOUR = "hh";
    /**
     * Capturing group name for minute of hour.
     */
    public static final String MINUTE = "mm";
    /**
     * Capturing group name for second of minute.
     */
    public static final String SECOND = "ss";

    private static final int BASE_YEAR_OFFSET = 98;

    private final Matcher matcher;
    private final ZoneId timestampZone;

    /**
     * Construct new matcher instance.
     *
     * @param filename
     *         file name to match against the {@code pattern}
     * @param pattern
     *         pattern to match the {@code filename} against
     * @param timestampZone
     *         time zone to apply on timestamp parsed from file name.
     *         This time zone is used for all time-related operations within this class.
     *
     * @throws IllegalArgumentException
     *         if provided {@code filename} does not match against provided {@code pattern}
     */
    public FilenameMatcher(final String filename, final Pattern pattern, final ZoneId timestampZone) {
        requireNonNull(filename, "filename");
        requireNonNull(pattern, "pattern");
        this.timestampZone = requireNonNull(timestampZone, "timestampZone");

        this.matcher = pattern.matcher(filename);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("No match found in filename " + filename + " with pattern " + pattern);
        }
    }

    /**
     * Return the subsequence of matched file name captured by provided {@code groupName} named-capturing group, if such group was matched.
     * Empty optional is returned if there is no match or pattern does not contain such group name.
     *
     * @param groupName
     *         capturing group name
     *
     * @return matched file name subsequence, or empty
     *
     * @see Matcher#group(String)
     */
    public Optional<String> getString(final String groupName) {
        requireNonNull(groupName, "groupName");
        try {
            return Optional.ofNullable(matcher.group(groupName));
        } catch (final RuntimeException ignored) {
            return Optional.empty();
        }
    }

    /**
     * Return the subsequence of matched file name captured by provided {@code groupName} named-capturing group parsed as an integer, if such group was
     * matched.
     * Empty optional is returned if matched subsequence is not parseable as an integer, there is no match or pattern does not contain such group name.
     *
     * @param groupName
     *         capturing group name
     *
     * @return matched file name subsequence, or empty
     *
     * @see Matcher#group(String)
     */
    public OptionalInt getInt(final String groupName) {
        requireNonNull(groupName, "groupName");
        try {
            return OptionalInt.of(Integer.parseInt(matcher.group(groupName)));
        } catch (final RuntimeException ignored) {
            return OptionalInt.empty();
        }
    }

    /**
     * Return partial and/or complete time instant parsed from timestamp fields in the file name, if available.
     * Timestamp fields must match following named capturing groups in the pattern:
     *
     * <table>
     *     <thead>
     *         <tr><th>Timestamp field</th><th>Capturing group name</th></tr>
     *     </thead>
     *     <tbody>
     *         <tr><td>year</td><td>{@value YEAR} (four-digit) or {@value TWO_DIGIT_YEAR} (two-digit)</td></tr>
     *         <tr><td>month of year</td><td>{@value MONTH}</td></tr>
     *         <tr><td>day of month</td><td>{@value DAY}</td></tr>
     *         <tr><td>hour of day</td><td>{@value HOUR}</td></tr>
     *         <tr><td>minute of hour</td><td>{@value MINUTE}</td></tr>
     *         <tr><td>second of minute</td><td>{@value SECOND}</td></tr>
     *     </tbody>
     * </table>
     *
     * <p>
     * This method reads matched timestamp fields and attempts to construct a {@link PartialOrCompleteTimeInstant#getCompleteTime() complete} and a
     * {@link PartialOrCompleteTimeInstant#getPartialTime() partial} time instant objects, if required temporal field values exist. At least all
     * {@link LocalDate date} fields are required for complete time. Missing {@link java.time.LocalTime time} fields will default to zero. In case a four-digit
     * year is unavailable, this method attempts to read two-digit year and complete it to the latest year that is at most one year ahead of current year by
     * the provided {@code clock}. If neither complete nor partial instant can be constructed, this method returns an empty optional.
     * </p>
     *
     * <p>
     * All time-related operations are made in the time zone this {@code FilenameMatcher} represents.
     * </p>
     *
     * @param clock
     *         clock providing current time
     *
     * @return parsed instant or empty
     */
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
                final LocalDate baseDate = ZonedDateTime.now(clock)//
                        .withZoneSameInstant(timestampZone)//
                        .minusYears(BASE_YEAR_OFFSET)//
                        .toLocalDate();
                final TemporalAccessor parsedYear = new DateTimeFormatterBuilder()
                        // The next year is the furthest year we allow
                        .appendValueReduced(ChronoField.YEAR, 2, 2, baseDate)//
                        .toFormatter()//
                        .parse(twoDigitYear);
                temporalFieldValues.put(ChronoField.YEAR, parsedYear.get(ChronoField.YEAR));
            });
        }

        return TimeUtil.toPartialOrCompleteTimeInstant(temporalFieldValues, timestampZone);
    }

}
