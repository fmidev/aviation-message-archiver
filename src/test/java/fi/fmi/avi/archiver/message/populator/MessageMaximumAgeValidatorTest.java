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

public class MessageMaximumAgeValidatorTest {

    private static final Clock clock = Clock.fixed(Instant.parse("2019-05-10T00:00:00Z"), ZoneId.of("UTC"));
    private MessageMaximumAgeValidator messageMaximumAgeValidator;
    private final InputAviationMessage inputAviationMessage = InputAviationMessage.builder().buildPartial();

    @BeforeEach
    public void setUp() {
        messageMaximumAgeValidator = new MessageMaximumAgeValidator(clock, Duration.ofDays(3));
    }

    @Test
    public void invalid_configuration_zero_duration() {
        assertThrows(IllegalArgumentException.class, () -> new MessageMaximumAgeValidator(clock, Duration.ZERO));
    }

    @Test
    public void invalid_configuration_negative_duration() {
        assertThrows(IllegalArgumentException.class, () -> new MessageMaximumAgeValidator(clock, Duration.ofDays(-1)));
    }

    @Test
    public void valid() {
        final ArchiveAviationMessage.Builder builder = createArchiveAviationMessage(clock.instant());
        messageMaximumAgeValidator.populate(inputAviationMessage, builder);
        assertThat(builder.getProcessingResult()).isEqualTo(ProcessingResult.OK);
    }

    @Test
    public void two_days_in_the_past() {
        final ArchiveAviationMessage.Builder builder = createArchiveAviationMessage(clock.instant().minus(2, ChronoUnit.DAYS));
        messageMaximumAgeValidator.populate(inputAviationMessage, builder);
        assertThat(builder.getProcessingResult()).isEqualTo(ProcessingResult.OK);
    }

    @Test
    public void three_days_in_the_past() {
        final ArchiveAviationMessage.Builder builder = createArchiveAviationMessage(clock.instant().minus(3, ChronoUnit.DAYS));
        messageMaximumAgeValidator.populate(inputAviationMessage, builder);
        assertThat(builder.getProcessingResult()).isEqualTo(ProcessingResult.OK);
    }

    @Test
    public void three_days_and_one_second_in_the_past() {
        final ArchiveAviationMessage.Builder builder = createArchiveAviationMessage(Instant.parse("2019-05-06T23:59:59Z"));
        messageMaximumAgeValidator.populate(inputAviationMessage, builder);
        assertThat(builder.getProcessingResult()).isEqualTo(ProcessingResult.MESSAGE_TOO_OLD);
    }

    @Test
    public void three_days_and_one_nanosecond_in_the_past() {
        final ArchiveAviationMessage.Builder builder = createArchiveAviationMessage(Instant.parse("2019-05-07T00:00:00Z").minusNanos(1));
        messageMaximumAgeValidator.populate(inputAviationMessage, builder);
        assertThat(builder.getProcessingResult()).isEqualTo(ProcessingResult.MESSAGE_TOO_OLD);
    }

    @Test
    public void year_in_the_past() {
        final ArchiveAviationMessage.Builder builder = createArchiveAviationMessage(Instant.parse("2018-05-10T00:00:00Z"));
        messageMaximumAgeValidator.populate(inputAviationMessage, builder);
        assertThat(builder.getProcessingResult()).isEqualTo(ProcessingResult.MESSAGE_TOO_OLD);
    }

    @Test
    public void twelve_hours_in_the_future() {
        final ArchiveAviationMessage.Builder builder = createArchiveAviationMessage(clock.instant().plus(12, ChronoUnit.HOURS));
        messageMaximumAgeValidator.populate(inputAviationMessage, builder);
        assertThat(builder.getProcessingResult()).isEqualTo(ProcessingResult.OK);
    }

    private static ArchiveAviationMessage.Builder createArchiveAviationMessage(final Instant messageTime) {
        return ArchiveAviationMessage.builder().setMessageTime(messageTime);
    }

}
