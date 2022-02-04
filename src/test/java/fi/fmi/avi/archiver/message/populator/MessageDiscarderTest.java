package fi.fmi.avi.archiver.message.populator;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.MessageDiscardedException;

public class MessageDiscarderTest {
    @Test
    void populate_discards_message() {
        final MessageDiscarder messageDiscarder = new MessageDiscarder();
        final InputAviationMessage input = InputAviationMessage.builder().buildPartial();
        final ArchiveAviationMessage.Builder target = ArchiveAviationMessage.builder();
        Assertions.assertThatExceptionOfType(MessageDiscardedException.class)//
                .isThrownBy(() -> messageDiscarder.populate(input, target));
    }
}
