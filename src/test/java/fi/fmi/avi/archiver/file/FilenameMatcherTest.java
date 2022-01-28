package fi.fmi.avi.archiver.file;

import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class FilenameMatcherTest {

    private static final ZoneId UTC = ZoneId.of("Z");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2022-02-10T00:00:00Z"), UTC);

    private static final Pattern FOUR_DIGIT_YEAR = Pattern
            .compile("^TAF_(?<dd>\\d{2})(?<MM>\\d{2})(?<yyyy>\\d{4})_(?<hh>\\d{2})(?<mm>\\d{2})(?<ss>\\d{2})_\\d{8}\\.xml$");
    private static final Pattern TWO_DIGIT_YEAR = Pattern
            .compile("^TAF_(?<dd>\\d{2})(?<MM>\\d{2})(?<yy>\\d{2})_(?<hh>\\d{2})(?<mm>\\d{2})(?<ss>\\d{2})_\\d{8}\\.xml$");


    @ParameterizedTest
    @MethodSource("testArguments")
    void populates_route(final String filename, final Pattern pattern, final ZonedDateTime expected) {
        final FilenameMatcher matcher = new FilenameMatcher(filename, pattern, UTC);
        final Optional<PartialOrCompleteTimeInstant> timestamp = matcher.getTimestamp(CLOCK);
        assertThat(timestamp).isPresent();
        final Optional<ZonedDateTime> completeTime = timestamp.get().getCompleteTime();
        assertThat(completeTime).hasValue(expected);
    }

    private static Stream<Arguments> testArguments() {
        return Stream.of(
                // Four digit year
                Arguments.of("TAF_13011965_024107_00007194.xml", FOUR_DIGIT_YEAR, ZonedDateTime.parse("1965-01-13T02:41:07Z")),
                Arguments.of("TAF_13012021_024107_00007194.xml", FOUR_DIGIT_YEAR, ZonedDateTime.parse("2021-01-13T02:41:07Z")),
                Arguments.of("TAF_13012022_024107_00007194.xml", FOUR_DIGIT_YEAR, ZonedDateTime.parse("2022-01-13T02:41:07Z")),
                Arguments.of("TAF_13012023_024107_00007194.xml", FOUR_DIGIT_YEAR, ZonedDateTime.parse("2023-01-13T02:41:07Z")),
                Arguments.of("TAF_13012024_024107_00007194.xml", FOUR_DIGIT_YEAR, ZonedDateTime.parse("2024-01-13T02:41:07Z")),
                Arguments.of("TAF_13012099_024107_00007194.xml", FOUR_DIGIT_YEAR, ZonedDateTime.parse("2099-01-13T02:41:07Z")),

                // Two digit year
                Arguments.of("TAF_130165_024107_00007194.xml", TWO_DIGIT_YEAR, ZonedDateTime.parse("1965-01-13T02:41:07Z")),
                Arguments.of("TAF_130121_024107_00007194.xml", TWO_DIGIT_YEAR, ZonedDateTime.parse("2021-01-13T02:41:07Z")),
                Arguments.of("TAF_130122_024107_00007194.xml", TWO_DIGIT_YEAR, ZonedDateTime.parse("2022-01-13T02:41:07Z")),
                Arguments.of("TAF_130123_024107_00007194.xml", TWO_DIGIT_YEAR, ZonedDateTime.parse("2023-01-13T02:41:07Z")),
                Arguments.of("TAF_130124_024107_00007194.xml", TWO_DIGIT_YEAR, ZonedDateTime.parse("1924-01-13T02:41:07Z")),
                Arguments.of("TAF_130199_024107_00007194.xml", TWO_DIGIT_YEAR, ZonedDateTime.parse("1999-01-13T02:41:07Z"))
        );
    }

}
