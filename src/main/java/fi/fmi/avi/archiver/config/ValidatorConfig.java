package fi.fmi.avi.archiver.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.messaging.MessageChannel;

import fi.fmi.avi.archiver.message.MessageValidator;
import fi.fmi.avi.archiver.message.MessageValidatorService;

@Configuration
public class ValidatorConfig {

    @Autowired
    private MessageChannel validatorChannel;

    @Autowired
    private MessageChannel archiveChannel;

    @Autowired
    private List<MessageValidator> messageValidators;

    // This is a placeholder validator that is only used when the application is launched without external message validator configuration
    @Bean
    public MessageValidator exampleValidator() {
        return aviationMessage -> {
        };
    }

    @Bean
    public MessageValidatorService messageValidatorService() {
        return new MessageValidatorService(messageValidators);
    }

    @Bean
    public IntegrationFlow validatorFlow() {
        return IntegrationFlows.from(validatorChannel)//
                .handle(messageValidatorService())//
                .channel(archiveChannel)//
                .get();
    }

}
