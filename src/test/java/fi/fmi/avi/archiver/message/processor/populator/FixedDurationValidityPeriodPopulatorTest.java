package fi.fmi.avi.archiver.message.processor.populator;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.processor.MessageProcessorContext;
import fi.fmi.avi.archiver.message.processor.TestMessageProcessorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.*;

@SuppressFBWarnings("UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR")
public class FixedDurationValidityPeriodPopulatorTest {

    private static final InputAviationMessage INPUT = InputAviationMessage.builder().buildPartial();
    private static final MessageProcessorContext CONTEXT = TestMessageProcessorContext.create(INPUT);
    private static final Duration VALIDITY_END_OFFSET = Duration.ofHours(30);

    private FixedDurationValidityPeriodPopulator messagePopulator;

    @BeforeEach
    public void setUp() {
        messagePopulator = new FixedDurationValidityPeriodPopulator(VALIDITY_END_OFFSET);
    }

    @Test
    void validity_end() {
        final Instant messageTime = Instant.parse("2019-05-10T00:00:00Z");
        final ArchiveAviationMessage.Builder target = ArchiveAviationMessage.builder()//
                .setMessageTime(messageTime);
        messagePopulator.populate(CONTEXT, target);
        assertThat(target.getValidFrom()).contains(messageTime);
        assertThat(target.getValidTo()).contains(Instant.parse("2019-05-11T06:00:00Z"));
    }

    @Test
    void missing_message_time() {
        final ArchiveAviationMessage.Builder target = ArchiveAviationMessage.builder();
        messagePopulator.populate(CONTEXT, target);
        assertThat(target.getValidFrom()).isNotPresent();
        assertThat(target.getValidTo()).isNotPresent();
    }

    @Test
    void constructor_given_positive_duration_constructs_instance() {
        assertThatNoException()//
                .isThrownBy(() -> new FixedDurationValidityPeriodPopulator(Duration.ofNanos(1)));
    }

    @Test
    void constructor_given_zero_duration_throws_exception() {
        assertThatIllegalArgumentException()//
                .isThrownBy(() -> new FixedDurationValidityPeriodPopulator(Duration.ZERO));
    }

    @Test
    void constructor_given_negative_duration_throws_exception() {
        assertThatIllegalArgumentException()//
                .isThrownBy(() -> new FixedDurationValidityPeriodPopulator(Duration.ofNanos(-1)));
    }
}
