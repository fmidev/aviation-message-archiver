package fi.fmi.avi.archiver.message;

import org.springframework.integration.annotation.ServiceActivator;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

public class MessageValidatorService {

    private final Collection<MessageValidator> validators;

    public MessageValidatorService(final Collection<MessageValidator> validators) {
        this.validators = requireNonNull(validators, "validators");
    }

    @ServiceActivator
    public List<AviationMessage> validateMessages(final List<AviationMessage> messages) {
        return messages.stream().map(this::validateMessage).collect(Collectors.toList());
    }

    private AviationMessage validateMessage(final AviationMessage message) {
        final AviationMessage.Builder messageBuilder = message.toBuilder();
        for (MessageValidator messageValidator : validators) {
            messageValidator.validate(messageBuilder);
            if (messageBuilder.getProcessingResult() != ProcessingResult.OK) {
                break;
            }
        }
        return messageBuilder.build();
    }

}
