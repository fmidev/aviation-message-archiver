package fi.fmi.avi.archiver.message;

import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.List;

import org.springframework.integration.annotation.ServiceActivator;

public class MessageValidatorService {

    private final Collection<MessageValidator> validators;

    public MessageValidatorService(final Collection<MessageValidator> validators) {
        this.validators = requireNonNull(validators, "validators");
    }

    @ServiceActivator
    public List<AviationMessage> validateMessages(final List<AviationMessage> messages) {
        messages.forEach(this::validateMessage);
        return messages;
    }

    private void validateMessage(final AviationMessage message) {
        validators.forEach(validator -> validator.validate(message));
    }

}
