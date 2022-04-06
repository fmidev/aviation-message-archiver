package fi.fmi.avi.archiver.config;

import java.time.Duration;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fi.fmi.avi.archiver.config.model.AviationProduct;
import fi.fmi.avi.archiver.spring.healthcontributor.BlockingExecutorHealthContributor;
import fi.fmi.avi.archiver.spring.healthcontributor.DirectoryPermissionHealthContributor;

@Configuration
public class HealthEndpointConfig {

    @Bean
    DirectoryPermissionHealthContributor directoryPermissionHealthContributor(final Map<String, AviationProduct> aviationProducts,
            @Value("${health-indicator.directory-permission.temp-file-prefix}") final String tempFilePrefix,
            @Value("${health-indicator.directory-permission.temp-file-suffix}") final String tempFileSuffix) {
        return new DirectoryPermissionHealthContributor(aviationProducts, tempFilePrefix, tempFileSuffix);
    }

    @Bean
    BlockingExecutorHealthContributor executorHealthContributor(
            @Value("${health-indicator.blocking-executor.timeout}") final Duration blockingExecutorTimeout) {
        return new BlockingExecutorHealthContributor(blockingExecutorTimeout);
    }

}
