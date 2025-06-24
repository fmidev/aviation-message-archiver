package fi.fmi.avi.archiver.config;

import com.google.common.collect.BiMap;
import fi.fmi.avi.archiver.config.model.AviationProduct;
import fi.fmi.avi.archiver.message.processor.conditional.TestInputValidFromReader;
import fi.fmi.avi.archiver.message.processor.conditional.TestInputValidToReader;
import fi.fmi.avi.archiver.message.processor.conditional.TestProductIdentifierPropertyReader;
import fi.fmi.avi.archiver.message.processor.conditional.TestTargetMessageTypePropertyReader;
import fi.fmi.avi.model.MessageType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.Map;

@Configuration
@Profile("integration-test")
public class MessageProcessorConditionPropertyReaderTestConfig {
    @Bean
    TestProductIdentifierPropertyReader testProductIdentifierPropertyReader(final Map<String, AviationProduct> aviationProducts) {
        return new TestProductIdentifierPropertyReader(aviationProducts.keySet());
    }

    @Bean
    TestTargetMessageTypePropertyReader testTargetMessageTypePropertyReader(final BiMap<MessageType, Integer> messageTypeIds) {
        return new TestTargetMessageTypePropertyReader(messageTypeIds);
    }

    @Bean
    TestInputValidFromReader testInputValidFromReader() {
        return new TestInputValidFromReader();
    }

    @Bean
    TestInputValidToReader testInputValidToReader() {
        return new TestInputValidToReader();
    }
}
