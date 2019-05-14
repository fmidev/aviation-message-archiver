package fi.fmi.avi.archiver;

import java.time.Clock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.config.EnableIntegration;

@Configuration
@EnableIntegration
@ComponentScan
@EnableAutoConfiguration
public class AviationMessageArchiver {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean(destroyMethod = "destroy")
    public ThreadGroup aviationMessageArchiverThreadGroup() {
        return new ThreadGroup(AviationMessageArchiver.class.getSimpleName());
    }

    public static void main(final String[] args) {
        SpringApplication.run(AviationMessageArchiver.class, args);
    }

}
