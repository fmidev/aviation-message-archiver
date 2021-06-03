package fi.fmi.avi.archiver.message;

import org.springframework.integration.annotation.ServiceActivator;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

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
        final AviationMessage.Builder messageBuilder = message.toBuilder();
        for (final MessageModifier messageModifier : modifiers) {
            messageModifier.modify(messageBuilder);
        }
        return messageBuilder.build();
    }

}
