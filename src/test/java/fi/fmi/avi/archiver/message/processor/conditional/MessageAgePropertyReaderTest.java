package fi.fmi.avi.archiver.message.processor.conditional;

import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessageOrBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import javax.annotation.Nullable;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class MessageAgePropertyReaderTest {

    private static final Clock utcClock = Clock.systemUTC();

    private static Stream<Duration> durationProvider() {
        return Stream.of(
                Duration.ZERO,
                Duration.ofSeconds(1),
                Duration.ofMinutes(5),
                Duration.ofHours(1),
                Duration.ofDays(7),
                Duration.ofMillis(12345),
                Duration.ofSeconds(-30)
        );
    }

    @Test
    void readValue_given_past_message_time_returns_positive_duration() {
        final Instant now = Instant.parse("2020-01-01T00:00:10Z");
        final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
        final MessageAgePropertyReader propertyReader = new MessageAgePropertyReader(clock);

        final InputAviationMessage input = InputAviationMessage.builder().buildPartial();
        final ArchiveAviationMessage.Builder message = ArchiveAviationMessage.builder()
                .setMessageTime(Instant.parse("2020-01-01T00:00:00Z"));

        final Duration result = propertyReader.readValue(input, message);

        assertThat(result).isEqualTo(Duration.ofSeconds(10));
    }

    @Test
    void readValue_given_same_message_time_as_now_returns_zero() {
        final Instant now = Instant.parse("2020-01-01T12:34:56Z");
        final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
        final MessageAgePropertyReader propertyReader = new MessageAgePropertyReader(clock);

        final InputAviationMessage input = InputAviationMessage.builder().buildPartial();
        final ArchiveAviationMessage.Builder message = ArchiveAviationMessage.builder()
                .setMessageTime(now);

        final Duration result = propertyReader.readValue(input, message);

        assertThat(result).isZero();
    }

    @Test
    void readValue_given_future_message_time_returns_negative_duration() {
        final Instant now = Instant.parse("2020-01-01T00:00:00Z");
        final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
        final MessageAgePropertyReader propertyReader = new MessageAgePropertyReader(clock);

        final InputAviationMessage input = InputAviationMessage.builder().buildPartial();
        final ArchiveAviationMessage.Builder message = ArchiveAviationMessage.builder()
                .setMessageTime(Instant.parse("2020-01-01T00:00:05Z"));

        final Duration result = propertyReader.readValue(input, message);

        assertThat(result).isEqualTo(Duration.ofSeconds(-5));
    }

    @ParameterizedTest
    @MethodSource("durationProvider")
    void validate_given_any_duration_returns_true(final Duration duration) {
        final MessageAgePropertyReader propertyReader = new MessageAgePropertyReader(utcClock);

        final boolean result = propertyReader.validate(duration);

        assertThat(result).isTrue();
    }

    @Test
    void testGetValueGetterForType() {
        final class TestReader extends AbstractConditionPropertyReader<Duration> {
            @Nullable
            @Override
            public Duration readValue(final InputAviationMessage input, final ArchiveAviationMessageOrBuilder message) {
                return null;
            }
        }
        final MessageAgePropertyReader reader = new MessageAgePropertyReader(utcClock);
        final TestReader controlReader = new TestReader();

        assertThat(reader.getValueGetterForType().getGenericReturnType())
                .isEqualTo(controlReader.getValueGetterForType().getGenericReturnType());
    }

    @Test
    void testGetPropertyName() {
        final class MessageAgePropertyReader extends ConditionPropertyReaderTests.AbstractTestStringConditionPropertyReader {
            public MessageAgePropertyReader() {
            }
        }

        final fi.fmi.avi.archiver.message.processor.conditional.MessageAgePropertyReader reader =
                new fi.fmi.avi.archiver.message.processor.conditional.MessageAgePropertyReader(utcClock);
        final MessageAgePropertyReader controlReader = new MessageAgePropertyReader();

        assertThat(reader.getPropertyName()).isEqualTo(controlReader.getPropertyName());
    }

    @Test
    void testToString() {
        final MessageAgePropertyReader propertyReader = new MessageAgePropertyReader(utcClock);

        assertThat(propertyReader.toString()).isEqualTo(propertyReader.getPropertyName());
    }
}
