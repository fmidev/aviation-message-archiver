package fi.fmi.avi.archiver.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.ErrorMessage;

import fi.fmi.avi.archiver.transformer.HeaderToFileTransformer;

@Configuration
public class ErrorMessageConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(ErrorMessageConfig.class);

    @Autowired
    private MessageChannel errorMessageChannel;

    @Autowired
    private MessageChannel failChannel;

    @Bean
    public IntegrationFlow errorMessageFlow() {
        final HeaderToFileTransformer toFile = new HeaderToFileTransformer();
        return IntegrationFlows.from(errorMessageChannel)//
                .handle(this)//
                .transform(toFile)//
                .channel(failChannel)//
                .get();
    }

    @ServiceActivator
    public Message<?> errorMessageToOriginalMessage(final ErrorMessage errorMessage) {
        if (errorMessage.getPayload() instanceof MessagingException) {
            return ((MessagingException) errorMessage.getPayload()).getFailedMessage();
        }
        // Attempt to use original message if the exception is not a MessagingException
        else if (errorMessage.getOriginalMessage() != null) {
            return errorMessage.getOriginalMessage();
        }
        // Unable to get the original message, log the exception
        LOGGER.error("Unable to extract message from error message", errorMessage.getPayload());
        return null;
    }

}
