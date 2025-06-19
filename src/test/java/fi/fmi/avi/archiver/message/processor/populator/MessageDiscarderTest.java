package fi.fmi.avi.archiver.message.processor.populator;

import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.MessageDiscardedException;
import fi.fmi.avi.archiver.message.processor.MessageProcessorContext;
import fi.fmi.avi.archiver.message.processor.TestMessageProcessorContext;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class MessageDiscarderTest {
    @Test
    void populate_discards_message() {
        final MessageDiscarder messageDiscarder = new MessageDiscarder();
        final MessageProcessorContext context = TestMessageProcessorContext.create(InputAviationMessage.builder().buildPartial());
        final ArchiveAviationMessage.Builder target = ArchiveAviationMessage.builder();
        Assertions.assertThatExceptionOfType(MessageDiscardedException.class)//
                .isThrownBy(() -> messageDiscarder.populate(context, target));
    }
}
