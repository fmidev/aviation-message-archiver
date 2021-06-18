package fi.fmi.avi.archiver.message.modifier;

import fi.fmi.avi.archiver.file.FileAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import org.springframework.integration.annotation.ServiceActivator;

import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

public class MessageModifierService {

    private final List<MessageModifier> modifiers;

    public MessageModifierService(final List<MessageModifier> modifiers) {
        this.modifiers = requireNonNull(modifiers, "modifiers");
    }

    @ServiceActivator
    public List<ArchiveAviationMessage> modifyMessages(final List<FileAviationMessage> messages) {
        return messages.stream().map(this::modifyMessage).collect(Collectors.toList());
    }

    private ArchiveAviationMessage modifyMessage(final FileAviationMessage fileAviationMessage) {
        final ArchiveAviationMessage.Builder messageBuilder = ArchiveAviationMessage.builder();
        for (final MessageModifier messageModifier : modifiers) {
            messageModifier.modify(fileAviationMessage, messageBuilder);
        }
        return messageBuilder.build();
    }

}
