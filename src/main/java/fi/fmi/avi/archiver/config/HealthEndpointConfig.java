package fi.fmi.avi.archiver.config;

import fi.fmi.avi.archiver.initializing.AviationProductsHolder;
import fi.fmi.avi.archiver.spring.healthcontributor.BlockingExecutorHealthContributor;
import fi.fmi.avi.archiver.spring.healthcontributor.DirectoryPermissionHealthContributor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class HealthEndpointConfig {

    @Bean
    public DirectoryPermissionHealthContributor directoryPermissionHealthContributor(final AviationProductsHolder aviationProductsHolder,
                                                                                     @Value("${health-indicator.directoryPermission.tempFilePrefix}") final String tempFilePrefix,
                                                                                     @Value("${health-indicator.directoryPermission.tempFileSuffix}") final String tempFileSuffix) {
        return new DirectoryPermissionHealthContributor(aviationProductsHolder, tempFilePrefix, tempFileSuffix);
    }

    @Bean
    public BlockingExecutorHealthContributor executorHealthContributor(@Value("${health-indicator.blockingExecutor.timeout}") final Duration blockingExecutorTimeout) {
        return new BlockingExecutorHealthContributor(blockingExecutorTimeout);
    }

}
