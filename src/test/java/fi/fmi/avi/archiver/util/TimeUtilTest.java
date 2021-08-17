package fi.fmi.avi.archiver.util;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import fi.fmi.avi.model.PartialDateTime;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;

class TimeUtilTest {
    static Stream<Arguments> testToCompleteTime() {
        return Stream.of(//
                Arguments.of(partialOrCompleteInstants(), null), //
                Arguments.of(partialOrCompleteInstants("--02T03:04:Z", "--03T04:05:Z"), null), //
                Arguments.of(partialOrCompleteInstants("2000-01-02T03:04:05.123456789Z", "2001-02-03T04:05:06Z", "2002-03-04T05:06:07Z"), //
                        ZonedDateTime.parse("2000-01-02T03:04:05.123456789Z")), //
                Arguments.of(partialOrCompleteInstants("--02T03:04:Z", "2001-02-03T04:05:06Z", "2002-03-04T05:06:07Z"), //
                        ZonedDateTime.parse("2001-02-02T03:04Z")), //
                Arguments.of(partialOrCompleteInstants("--T03:04:Z", "--03T04:05:Z", "2002-03-04T05:06:07Z"), //
                        ZonedDateTime.parse("2002-03-03T03:04Z"))//
        );
    }

    private static List<PartialOrCompleteTimeInstant> partialOrCompleteInstants(final String... source) {
        return Arrays.stream(source)//
                .map(ZonedChronoFieldValues::parse)//
                .map(values -> TimeUtil.toPartialOrCompleteTimeInstant(values.getChronoFieldValues(), values.getZoneId().orElse(null)).orElse(null))//
                .collect(Collectors.toList());
    }

    @ParameterizedTest
    @CsvSource({//
            "--T::Z, ", //
            "2000--T::Z, ", //
            "-04-T::Z, ", //
            "--07T::Z, ", //
            "2001-02-T::Z, ", //
            "2002--03T::Z, ", //
            "2003-03-04T::Z, 2003-03-04T00:00:00Z", //
            "2003-03-04T05::Z, 2003-03-04T05:00:00Z", //
            "2003-03-04T:06:Z, 2003-03-04T00:06:00Z", //
            "2003-03-04T::07Z, 2003-03-04T00:00:07Z", //
            "2003-03-04T::.012345678Z, 2003-03-04T00:00:00.012345678Z", //
            "2003-03-04T05:06:07.012345678Z, 2003-03-04T05:06:07.012345678Z", //
            "2003-03-04T05:06:07.012345678+01:00, 2003-03-04T05:06:07.012345678+01:00", //
    })
    void testToZonedDateTime(final ZonedChronoFieldValues input, @Nullable final ZonedDateTime expectedResult) {
        final Optional<ZonedDateTime> actual = TimeUtil.toZonedDateTime(input.getChronoFieldValues(), input.getZoneId().orElseThrow(NullPointerException::new));
        assertThat(actual).isEqualTo(Optional.ofNullable(expectedResult));
    }

    @ParameterizedTest
    @CsvSource({//
            "--T::, ", //
            "2000--T::, ", //
            "-01-T::, ", //
            "--02T::, --02T::", //
            "--T03::, --T03::", //
            "--T:04:, --T:04:", //
            "--T::Z, ", //
            "--02T03:04:Z, --02T03:04:Z", //
            "2000-01-02T03:04:05.123456789Z, --02T03:04:Z", //
    })
    void testToPartialDateTime(final ZonedChronoFieldValues input, @Nullable final PartialDateTime expectedResult) {
        final Optional<PartialDateTime> actual = TimeUtil.toPartialDateTime(input.getChronoFieldValues(), input.getZoneId().orElse(null));
        assertThat(actual).isEqualTo(Optional.ofNullable(expectedResult));
    }

    @Test
    void toPartialOrCompleteTimeInstant_returns_empty_Optional_for_empty_input() {
        final Optional<PartialOrCompleteTimeInstant> actual = TimeUtil.toPartialOrCompleteTimeInstant(Collections.emptyMap(), ZoneOffset.UTC);
        assertThat(actual).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({//
            "2000-01-02T03:04:05.123456789Z, --02T03:04:Z, 2000-01-02T03:04:05.123456789Z", //
            "2000-01-02T::Z, --02T::Z, 2000-01-02T00:00:00Z", //
            "2000-01-T03::Z, --T03::Z, ", //
            "2000-01-02T03:04:05.123456789, --02T03:04:, ", //
    })
    void toPartialOrCompleteTimeInstant_populates_complete_and_or_partial_time_when_available(final ZonedChronoFieldValues input,
            @Nullable final PartialDateTime expectedPartialTime, @Nullable final ZonedDateTime expectedCompleteTime) {
        final PartialOrCompleteTimeInstant actual = TimeUtil.toPartialOrCompleteTimeInstant(input.getChronoFieldValues(), input.getZoneId().orElse(null))
                .orElse(null);
        assertThat(actual).isNotNull();
        assertThat(actual.getPartialTime().orElse(null)).as("partialTime").isEqualTo(expectedPartialTime);
        assertThat(actual.getCompleteTime().orElse(null)).as("completeTime").isEqualTo(expectedCompleteTime);
    }

    @ParameterizedTest
    @MethodSource
    void testToCompleteTime(final List<PartialOrCompleteTimeInstant> input, @Nullable final ZonedDateTime expectedResult) {
        assertThat(TimeUtil.toCompleteTime(input)).isEqualTo(Optional.ofNullable(expectedResult));
    }

    static final class ZonedChronoFieldValues {
        private static final List<ChronoField> CHRONO_FIELDS = Collections.unmodifiableList(
                Arrays.asList(ChronoField.YEAR, ChronoField.MONTH_OF_YEAR, ChronoField.DAY_OF_MONTH, ChronoField.HOUR_OF_DAY, ChronoField.MINUTE_OF_HOUR,
                        ChronoField.SECOND_OF_MINUTE, ChronoField.NANO_OF_SECOND));
        private static final Pattern PARTIAL_DATE_TIME_PATTERN = Pattern.compile(
                "(?<YEAR>\\d{4})?-(?<MONTH>\\d{2})?-(?<DAY>\\d{2})?T(?<HOUR>\\d{2})?:(?<MINUTE>\\d{2})?:(?<SECOND>\\d{2})?(?:\\.(?<NANO>\\d{9}))?"
                        + "(?<ZONE>[0-9A-Za-z/:+-]+)?");
        private static final Map<ChronoField, String> GROUP_NAMES = createGroupNames();

        private final Map<ChronoField, Integer> chronoFieldValues;
        @Nullable
        private final ZoneId zoneId;

        private ZonedChronoFieldValues(final Map<ChronoField, Integer> chronoFieldValues, @Nullable final ZoneId zoneId) {
            this.chronoFieldValues = requireNonNull(chronoFieldValues, "chronoFieldValues");
            this.zoneId = zoneId;
        }

        private static Map<ChronoField, String> createGroupNames() {
            final Map<ChronoField, String> names = new EnumMap<>(ChronoField.class);
            names.put(ChronoField.YEAR, "YEAR");
            names.put(ChronoField.MONTH_OF_YEAR, "MONTH");
            names.put(ChronoField.DAY_OF_MONTH, "DAY");
            names.put(ChronoField.HOUR_OF_DAY, "HOUR");
            names.put(ChronoField.MINUTE_OF_HOUR, "MINUTE");
            names.put(ChronoField.SECOND_OF_MINUTE, "SECOND");
            names.put(ChronoField.NANO_OF_SECOND, "NANO");
            return Collections.unmodifiableMap(names);
        }

        public static ZonedChronoFieldValues parse(final String string) {
            final Matcher matcher = PARTIAL_DATE_TIME_PATTERN.matcher(string);
            if (!matcher.matches()) {
                throw new IllegalArgumentException(String.format(Locale.ROOT, "<%s> does not match <%s>", string, PARTIAL_DATE_TIME_PATTERN));
            }
            final EnumMap<ChronoField, Integer> builder = new EnumMap<>(ChronoField.class);
            CHRONO_FIELDS.forEach(field -> putValue(builder, matcher, field));
            final Map<ChronoField, Integer> chronoFieldValues = Collections.unmodifiableMap(builder);
            @Nullable
            final ZoneId zoneId = Optional.ofNullable(matcher.group("ZONE")).map(ZoneId::of).orElse(null);
            return new ZonedChronoFieldValues(chronoFieldValues, zoneId);
        }

        private static void putValue(final Map<ChronoField, Integer> builder, final Matcher matcher, final ChronoField chronoField) {
            final String stringValue = matcher.group(GROUP_NAMES.get(chronoField));
            if (stringValue == null) {
                return;
            }
            builder.put(chronoField, Integer.valueOf(stringValue));
        }

        public Map<ChronoField, Integer> getChronoFieldValues() {
            return chronoFieldValues;
        }

        public Optional<ZoneId> getZoneId() {
            return Optional.ofNullable(zoneId);
        }
    }
}
