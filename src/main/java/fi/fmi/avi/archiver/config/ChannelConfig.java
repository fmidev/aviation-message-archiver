package fi.fmi.avi.archiver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.messaging.MessageChannel;

@Configuration
@EnableIntegration
public class ChannelConfig {

    @Bean
    public MessageChannel inputChannel() {
        return new PublishSubscribeChannel();
    }

    @Bean
    public MessageChannel processingChannel() {
        return new PublishSubscribeChannel();
    }

    @Bean
    public MessageChannel outputChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel errorChannel() {
        return new DirectChannel();
    }
}
