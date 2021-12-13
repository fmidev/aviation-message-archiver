package fi.fmi.avi.archiver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.config.EnableIntegration;

import java.time.Clock;

import static java.util.Objects.requireNonNull;

@Configuration
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

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean(destroyMethod = "destroy")
    public ThreadGroup aviationMessageArchiverThreadGroup() {
        return new ThreadGroup(AviationMessageArchiver.class.getSimpleName());
    }

}
