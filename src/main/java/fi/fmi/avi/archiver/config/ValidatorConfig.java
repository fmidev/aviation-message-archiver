package fi.fmi.avi.archiver.config;

import fi.fmi.avi.archiver.message.validator.MessageValidator;
import fi.fmi.avi.archiver.message.validator.MessageValidatorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.messaging.MessageChannel;

import java.util.List;

@Configuration
public class ValidatorConfig {

    @Autowired
    private MessageChannel validatorChannel;

    @Autowired
    private MessageChannel databaseChannel;

    @Autowired
    private List<MessageValidator> messageValidators;

    @Bean
    public MessageValidatorService messageValidatorService() {
        return new MessageValidatorService(messageValidators);
    }

    @Bean
    public IntegrationFlow validatorFlow() {
        return IntegrationFlows.from(validatorChannel)//
                .handle(messageValidatorService())//
                .channel(databaseChannel)//
                .get();
    }

}
