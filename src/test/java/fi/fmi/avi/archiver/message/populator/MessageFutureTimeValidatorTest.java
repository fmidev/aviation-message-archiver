package fi.fmi.avi.archiver.message.populator;

import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.ProcessingResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class MessageFutureTimeValidatorTest {

    private static final Clock clock = Clock.fixed(Instant.parse("2019-05-10T00:00:00Z"), ZoneId.of("UTC"));
    private MessageFutureTimeValidator messageFutureTimeValidator;
    private final InputAviationMessage inputAviationMessage = InputAviationMessage.builder().buildPartial();

    @BeforeEach
    public void setUp() {
        messageFutureTimeValidator = new MessageFutureTimeValidator(clock, Duration.ofHours(12));
    }

    @Test
    public void invalid_configuration_zero_duration() {
        assertThrows(IllegalArgumentException.class, () -> new MessageFutureTimeValidator(clock, Duration.ZERO));
    }

    @Test
    public void invalid_configuration_negative_duration() {
        assertThrows(IllegalArgumentException.class, () -> new MessageFutureTimeValidator(clock, Duration.ofDays(-1)));
    }

    @Test
    public void valid() {
        final ArchiveAviationMessage.Builder builder = createArchiveAviationMessage(Instant.parse("2019-05-10T00:00:00Z"));
        messageFutureTimeValidator.populate(inputAviationMessage, builder);
        assertThat(builder.getProcessingResult()).isEqualTo(ProcessingResult.OK);
    }

    @Test
    public void two_days_in_the_past() {
        final ArchiveAviationMessage.Builder builder = createArchiveAviationMessage(Instant.parse("2019-05-08T00:00:00Z"));
        messageFutureTimeValidator.populate(inputAviationMessage, builder);
        assertThat(builder.getProcessingResult()).isEqualTo(ProcessingResult.OK);
    }

    @Test
    public void twelve_hours_minus_nanosecond_in_the_future() {
        final ArchiveAviationMessage.Builder builder = createArchiveAviationMessage(Instant.parse("2019-05-10T12:00:00Z")
                .minusNanos(1));
        messageFutureTimeValidator.populate(inputAviationMessage, builder);
        assertThat(builder.getProcessingResult()).isEqualTo(ProcessingResult.OK);
    }

    @Test
    public void twelve_hours_in_the_future() {
        final ArchiveAviationMessage.Builder builder = createArchiveAviationMessage(Instant.parse("2019-05-10T12:00:00Z"));
        messageFutureTimeValidator.populate(inputAviationMessage, builder);
        assertThat(builder.getProcessingResult()).isEqualTo(ProcessingResult.MESSAGE_TIME_IN_FUTURE);
    }

    @Test
    public void twelve_hours_and_one_nanosecond_in_the_future() {
        final ArchiveAviationMessage.Builder builder = createArchiveAviationMessage(clock.instant()
                .plus(12, ChronoUnit.HOURS).plusNanos(1));
        messageFutureTimeValidator.populate(inputAviationMessage, builder);
        assertThat(builder.getProcessingResult()).isEqualTo(ProcessingResult.MESSAGE_TIME_IN_FUTURE);
    }

    @Test
    public void two_days_in_the_future() {
        final ArchiveAviationMessage.Builder builder = createArchiveAviationMessage(Instant.parse("2019-05-12T00:00:00Z"));
        messageFutureTimeValidator.populate(inputAviationMessage, builder);
        assertThat(builder.getProcessingResult()).isEqualTo(ProcessingResult.MESSAGE_TIME_IN_FUTURE);
    }

    private static ArchiveAviationMessage.Builder createArchiveAviationMessage(final Instant messageTime) {
        return ArchiveAviationMessage.builder().setMessageTime(messageTime);
    }

}
