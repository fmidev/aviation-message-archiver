package fi.fmi.avi.archiver.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.messaging.MessageChannel;

import fi.fmi.avi.archiver.message.MessageModifier;
import fi.fmi.avi.archiver.message.MessageModifierService;

@Configuration
public class ModifierConfig {

    @Autowired
    private MessageChannel modifierChannel;

    @Autowired
    private List<MessageModifier> messageModifiers;

    // This is a placeholder modifier that is only used when the application is launched without external message modifier configuration
    @Bean
    public MessageModifier exampleModifier() {
        return message -> message;
    }

    @Bean
    public MessageModifierService messageModifierService() {
        return new MessageModifierService(messageModifiers);
    }

    @Bean
    public IntegrationFlow modifierFlow() {
        return IntegrationFlows.from(modifierChannel).log("Modifier").get();
    }

}
