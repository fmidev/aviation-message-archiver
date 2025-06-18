package fi.fmi.avi.archiver.message.populator;

import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.MessageDiscardedException;
import fi.fmi.avi.archiver.message.MessageProcessorContext;
import fi.fmi.avi.archiver.message.TestMessageProcessorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

public class MessageContentTrimmerTest {
    private final MessageProcessorContext context = TestMessageProcessorContext.create(InputAviationMessage.builder().buildPartial());

    private MessagePopulator messagePopulator;

    @BeforeEach
    public void setUp() {
        messagePopulator = new MessageContentTrimmer();
    }

    @Test
    void trim() throws MessageDiscardedException {
        final ArchiveAviationMessage.Builder target = ArchiveAviationMessage.builder()//
                .setMessage("\nTAF TEST ")//
                .setHeading("")//
                .setStationIcaoCode("")//
                .setMessageTime(Instant.EPOCH)//
                .setType(2)//
                .setRoute(1);

        messagePopulator.populate(context, target);
        assertThat(target.getMessage()).isEqualTo("TAF TEST");
    }

}
