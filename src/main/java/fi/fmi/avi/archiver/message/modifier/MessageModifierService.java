package fi.fmi.avi.archiver.message.modifier;

import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import org.springframework.integration.annotation.ServiceActivator;

import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

public class MessageModifierService {

    private final List<MessagePopulator> modifiers;

    public MessageModifierService(final List<MessagePopulator> modifiers) {
        this.modifiers = requireNonNull(modifiers, "modifiers");
    }

    @ServiceActivator
    public List<ArchiveAviationMessage> modifyMessages(final List<InputAviationMessage> messages) {
        return messages.stream().map(this::modifyMessage).collect(Collectors.toList());
    }

    private ArchiveAviationMessage modifyMessage(final InputAviationMessage inputAviationMessage) {
        final ArchiveAviationMessage.Builder messageBuilder = ArchiveAviationMessage.builder();
        for (final MessagePopulator messagePopulator : modifiers) {
            messagePopulator.modify(inputAviationMessage, messageBuilder);
        }
        return messageBuilder.build();
    }

}
