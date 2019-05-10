package fi.fmi.avi.archiver.message;

import static java.util.Objects.requireNonNull;

import java.util.Collection;

public class MessageModifierService {

    private final Collection<MessageModifier> modifiers;

    public MessageModifierService(final Collection<MessageModifier> modifiers) {
        this.modifiers = requireNonNull(modifiers, "modifiers");
    }

    public Message modifyMessage(final Message message) {
        Message modifiedMessage = message;
        for (final MessageModifier messageModifier : modifiers) {
            modifiedMessage = messageModifier.modify(message);
        }
        return modifiedMessage;
    }

}
