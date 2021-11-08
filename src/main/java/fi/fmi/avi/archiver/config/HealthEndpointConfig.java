package fi.fmi.avi.archiver.config;

import fi.fmi.avi.archiver.initializing.AviationProductsHolder;
import fi.fmi.avi.archiver.spring.healthcontributor.DirectoryPermissionHealthContributor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HealthEndpointConfig {

    @Value("${health-indicator.directoryPermission.tempFilePrefix}")
    private String tempFilePrefix;

    @Value("${health-indicator.directoryPermission.tempFileSuffix}")
    private String tempFileSuffix;

    @Autowired
    private AviationProductsHolder aviationProductsHolder;

    @Bean
    public DirectoryPermissionHealthContributor directoryPermissionHealthContributor() {
        return new DirectoryPermissionHealthContributor(aviationProductsHolder, tempFilePrefix, tempFileSuffix);
    }

}
