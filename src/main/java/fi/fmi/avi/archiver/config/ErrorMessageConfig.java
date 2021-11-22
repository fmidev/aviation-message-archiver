package fi.fmi.avi.archiver.config;

import javax.annotation.Nullable;

import org.aopalliance.aop.Advice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.handler.advice.ExpressionEvaluatingRequestHandlerAdvice;
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
    private MessageChannel errorLoggingChannel;

    @Autowired
    private MessageChannel failChannel;

    @Autowired
    private MessageChannel finishChannel;

    @Bean
    public IntegrationFlow errorMessageFlow() {
        final HeaderToFileTransformer toFile = new HeaderToFileTransformer();
        return IntegrationFlows.from(errorMessageChannel)//
                .handle(this)//
                .transform(toFile)//
                .channel(failChannel)//
                .get();
    }

    @Bean
    public IntegrationFlow errorLoggingFlow() {
        return IntegrationFlows.from(errorLoggingChannel)//
                .handle(this)//
                .channel(finishChannel)//
                .get();
    }

    // Trap exceptions to avoid infinite looping when the error message flow itself results in exceptions
    @Bean
    public Advice exceptionTrapAdvice() {
        final ExpressionEvaluatingRequestHandlerAdvice advice = new ExpressionEvaluatingRequestHandlerAdvice();
        advice.setFailureChannel(errorLoggingChannel);
        advice.setTrapException(true);
        return advice;
    }

    @ServiceActivator
    @Nullable
    public Message<?> errorMessageToOriginalMessage(final ErrorMessage errorMessage) {
        final Message<?> failedMessage;
        if (errorMessage.getPayload() instanceof MessagingException) {
            failedMessage = ((MessagingException) errorMessage.getPayload()).getFailedMessage();
        }
        // Attempt to use original message if the exception is not a MessagingException
        else if (errorMessage.getOriginalMessage() != null) {
            failedMessage = errorMessage.getOriginalMessage();
        } else {
            // Unable to get the original message, log the exception
            LOGGER.error("Unable to extract message from error message", errorMessage.getPayload());
            return null;
        }
        LOGGER.error("Processing message {} failed: ", failedMessage, errorMessage.getPayload());
        return failedMessage;
    }

}
