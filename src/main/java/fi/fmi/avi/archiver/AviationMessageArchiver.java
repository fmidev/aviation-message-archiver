package fi.fmi.avi.archiver;

import static java.util.Objects.requireNonNull;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.integration.config.EnableIntegration;

@EnableIntegration
@ComponentScan
@ConfigurationPropertiesScan
@EnableAutoConfiguration
public class AviationMessageArchiver {
    public static void main(final String[] args) {
        requireNonNull(args, "args");
        // Set the application wide timezone to UTC. This is especially needed for the H2 database.
        System.setProperty("user.timezone", "UTC");
        SpringApplication.run(AviationMessageArchiver.class, args);
    }
}
