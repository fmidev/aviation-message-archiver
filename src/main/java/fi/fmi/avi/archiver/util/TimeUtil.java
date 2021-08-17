package fi.fmi.avi.archiver.util;

import static java.util.Objects.requireNonNull;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nullable;

import fi.fmi.avi.model.PartialDateTime;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;

public final class TimeUtil {
    private static final Set<ChronoField> DATE_FIELDS = Collections.unmodifiableSet(
            EnumSet.of(ChronoField.YEAR, ChronoField.MONTH_OF_YEAR, ChronoField.DAY_OF_MONTH));

    private TimeUtil() {
        throw new AssertionError();
    }

    /**
     * Construct a {@link ZonedDateTime} from provided {@code temporalFieldValues} and {@code zoneId}.
     * Only following temporal fields are supported:
     * {@link ChronoField#YEAR YEAR},
     * {@link ChronoField#MONTH_OF_YEAR MONTH_OF_YEAR},
     * {@link ChronoField#DAY_OF_MONTH DAY_OF_MONTH},
     * {@link ChronoField#HOUR_OF_DAY HOUR_OF_DAY},
     * {@link ChronoField#MINUTE_OF_HOUR MINUTE_OF_HOUR},
     * {@link ChronoField#SECOND_OF_MINUTE SECOND_OF_MINUTE} and
     * {@link ChronoField#NANO_OF_SECOND NANO_OF_SECOND}.
     * All unsupported fields are simply ignored.
     * All date fields must be present to construct a {@code ZonedDateTime} instance. Any missing time field values are replaced with zero ({@code 0}).
     * If an instance cannot be constructed, an empty {@code Optional} is returned.
     *
     * @param temporalFieldValues
     *         temporal field values
     * @param zoneId
     *         zone id
     *
     * @return constructed {@code ZonedDateTime} or empty if value cannot be constructed
     */
    public static Optional<ZonedDateTime> toZonedDateTime(final Map<ChronoField, Integer> temporalFieldValues, final ZoneId zoneId) {
        requireNonNull(temporalFieldValues, "temporalFieldValues");
        requireNonNull(zoneId, "zoneId");
        if (temporalFieldValues.keySet().containsAll(DATE_FIELDS)) {
            return Optional.of(ZonedDateTime.of(//
                    temporalFieldValues.get(ChronoField.YEAR), //
                    temporalFieldValues.get(ChronoField.MONTH_OF_YEAR), //
                    temporalFieldValues.get(ChronoField.DAY_OF_MONTH), //
                    temporalFieldValues.getOrDefault(ChronoField.HOUR_OF_DAY, 0), //
                    temporalFieldValues.getOrDefault(ChronoField.MINUTE_OF_HOUR, 0), //
                    temporalFieldValues.getOrDefault(ChronoField.SECOND_OF_MINUTE, 0), //
                    temporalFieldValues.getOrDefault(ChronoField.NANO_OF_SECOND, 0), //
                    zoneId));
        } else {
            return Optional.empty();
        }
    }

    /**
     * Construct a {@link PartialDateTime} from provided {@code temporalFieldValues} and {@code zoneId}.
     * Supports temporal fields available in {@code PartialDateTime}. All unsupported fields are simply ignored.
     * If no {@code temporalFieldValues} contains no supported field values, an empty {@code Optional} is returned.
     *
     * @param temporalFieldValues
     *         temporal field values
     * @param zoneId
     *         zone id or {@code null}
     *
     * @return constructed {@code PartialDateTime} or empty if value cannot be constructed
     */
    public static Optional<PartialDateTime> toPartialDateTime(final Map<ChronoField, Integer> temporalFieldValues, @Nullable final ZoneId zoneId) {
        requireNonNull(temporalFieldValues, "temporalFieldValues");
        final int day = getPartialFieldValue(temporalFieldValues, PartialDateTime.PartialField.DAY);
        final int hour = getPartialFieldValue(temporalFieldValues, PartialDateTime.PartialField.HOUR);
        final int minute = getPartialFieldValue(temporalFieldValues, PartialDateTime.PartialField.MINUTE);
        if (day < 0 && hour < 0 && minute < 0) {
            return Optional.empty();
        }
        return Optional.of(PartialDateTime.of(day, hour, minute, zoneId));
    }

    private static Integer getPartialFieldValue(final Map<ChronoField, Integer> temporalFieldValues, final PartialDateTime.PartialField partialField) {
        return temporalFieldValues.getOrDefault(partialField.getTemporalField(), -1);
    }

    /**
     * Construct a {@link PartialOrCompleteTimeInstant} from provided {@code temporalFieldValues} and {@code zoneId}.
     * Rules of {@link #toZonedDateTime(Map, ZoneId)} and {@link #toPartialDateTime(Map, ZoneId)} apply when constructing property values.
     * If resulting instance contains neither partial or complete time, an empty {@code Optional} is returned.
     *
     * @param temporalFieldValues
     *         temporal field values
     * @param zoneId
     *         zone id or {@code null}
     *
     * @return constructed {@code PartialOrCompleteTimeInstant} or empty if value cannot be constructed
     */
    public static Optional<PartialOrCompleteTimeInstant> toPartialOrCompleteTimeInstant(final Map<ChronoField, Integer> temporalFieldValues,
            @Nullable final ZoneId zoneId) {
        requireNonNull(temporalFieldValues, "temporalFieldValues");
        final PartialOrCompleteTimeInstant.Builder builder = PartialOrCompleteTimeInstant.builder()//
                .setPartialTime(toPartialDateTime(temporalFieldValues, zoneId));
        if (zoneId != null) {
            builder.setCompleteTime(toZonedDateTime(temporalFieldValues, zoneId));
        }
        if (builder.getPartialTime().isPresent() || builder.getCompleteTime().isPresent()) {
            return Optional.of(builder.build());
        } else {
            return Optional.empty();
        }
    }

    /**
     * Attempts to resolve complete time for the first of {@code times} returning the complete value if present, or completing the partial time using the
     * next appropriate in {@code times} for reference.
     * In case the reference is not complete but partial, reference completion is attempted recursively using following elements in {@code times} as reference.
     *
     * @param times
     *         times for completion
     *
     * @return the complete time if resolved, otherwise empty
     */
    public static Optional<ZonedDateTime> toCompleteTime(final PartialOrCompleteTimeInstant... times) {
        requireNonNull(times, "times");
        return toCompleteTime(Arrays.asList(times));
    }

    /**
     * Attempts to resolve complete time for the first of {@code times} returning the complete value if present, or completing the partial time using the
     * next appropriate in {@code times} for reference.
     * In case the reference is not complete but partial, reference completion is attempted recursively using following elements in {@code times} as reference.
     *
     * @param times
     *         times for completion
     *
     * @return the complete time if resolved, otherwise empty
     */
    public static Optional<ZonedDateTime> toCompleteTime(final Iterable<PartialOrCompleteTimeInstant> times) {
        requireNonNull(times, "times");
        return toCompleteTime(times.iterator());
    }

    private static Optional<ZonedDateTime> toCompleteTime(final Iterator<PartialOrCompleteTimeInstant> times) {
        requireNonNull(times, "times");
        final PartialOrCompleteTimeInstant toResolve = nextNonNullOrNull(times);
        if (toResolve == null) {
            return Optional.empty();
        }
        final Optional<ZonedDateTime> completeTimeOptional = toResolve.getCompleteTime();
        if (completeTimeOptional.isPresent()) {
            return completeTimeOptional;
        }
        return toResolve.getPartialTime()//
                .flatMap(partial -> toCompleteTime(times)//
                        .map(partial::toZonedDateTimeNear));
    }

    @Nullable
    private static <E> E nextNonNullOrNull(final Iterator<E> iterator) {
        while (iterator.hasNext()) {
            final E next = iterator.next();
            if (next != null) {
                return next;
            }
        }
        return null;
    }
}
