package fi.fmi.avi.archiver.config;

import fi.fmi.avi.archiver.message.modifier.MessageModifier;
import fi.fmi.avi.archiver.message.modifier.MessageModifierService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.messaging.MessageChannel;

import java.util.List;

@Configuration
public class ModifierConfig {

    @Autowired
    private MessageChannel modifierChannel;

    @Autowired
    private MessageChannel validatorChannel;

    @Autowired
    private List<MessageModifier> messageModifiers;

    @Bean
    public MessageModifierService messageModifierService() {
        return new MessageModifierService(messageModifiers);
    }

    @Bean
    public IntegrationFlow modifierFlow() {
        return IntegrationFlows.from(modifierChannel)//
                .handle(messageModifierService())//
                .channel(validatorChannel)//
                .get();
    }

}
