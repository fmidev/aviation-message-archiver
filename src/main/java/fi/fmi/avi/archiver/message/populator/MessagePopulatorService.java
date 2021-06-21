package fi.fmi.avi.archiver.message.populator;

import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import org.springframework.integration.annotation.ServiceActivator;

import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

public class MessagePopulatorService {

    private final List<MessagePopulator> messagePopulators;

    public MessagePopulatorService(final List<MessagePopulator> messagePopulators) {
        this.messagePopulators = requireNonNull(messagePopulators, "messagePopulators");
    }

    @ServiceActivator
    public List<ArchiveAviationMessage> populateMessages(final List<InputAviationMessage> messages) {
        return messages.stream().map(this::populateMessage).collect(Collectors.toList());
    }

    private ArchiveAviationMessage populateMessage(final InputAviationMessage inputAviationMessage) {
        final ArchiveAviationMessage.Builder messageBuilder = ArchiveAviationMessage.builder();
        for (final MessagePopulator messagePopulator : messagePopulators) {
            messagePopulator.populate(inputAviationMessage, messageBuilder);
        }
        return messageBuilder.build();
    }

}
