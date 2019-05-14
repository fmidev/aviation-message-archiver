package fi.fmi.avi.archiver.message;

import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.integration.annotation.ServiceActivator;

public class MessageModifierService {

    private final Collection<MessageModifier> modifiers;

    public MessageModifierService(final Collection<MessageModifier> modifiers) {
        this.modifiers = requireNonNull(modifiers, "modifiers");
    }

    @ServiceActivator
    public List<AviationMessage> modifyMessages(final List<AviationMessage> messages) {
        return messages.stream().map(this::modifyMessage).collect(Collectors.toList());
    }

    private AviationMessage modifyMessage(final AviationMessage message) {
        AviationMessage modifiedMessage = message;
        for (final MessageModifier messageModifier : modifiers) {
            modifiedMessage = messageModifier.modify(modifiedMessage);
        }
        return modifiedMessage;
    }

}
