package fi.fmi.avi.archiver.message.populator;

import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.MessageDiscardedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

public class MessageContentTrimmerTest {

    private MessagePopulator messagePopulator;
    private final InputAviationMessage inputAviationMessage = InputAviationMessage.builder().buildPartial();

    @BeforeEach
    public void setUp() {
        messagePopulator = new MessageContentTrimmer();
    }

    @Test
    public void trim() throws MessageDiscardedException {
        final ArchiveAviationMessage.Builder aviationMessage = ArchiveAviationMessage.builder()//
                .setMessage("\nTAF TEST ")//
                .setHeading("")//
                .setStationIcaoCode("")//
                .setMessageTime(Instant.EPOCH)//
                .setType(2)//
                .setRoute(1);

        messagePopulator.populate(inputAviationMessage, aviationMessage);
        assertThat(aviationMessage.getMessage()).isEqualTo("TAF TEST");
    }

}
