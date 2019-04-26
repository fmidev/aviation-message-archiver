package fi.fmi.avi.archiver;

import org.springframework.beans.factory.annotation.Value;
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

    public static void main(final String[] args) {
        SpringApplication.run(AviationMessageArchiver.class, args);
    }

    @Bean(destroyMethod = "destroy")
    public ThreadGroup aviationMessageServiceThreadGroup() {
        return new ThreadGroup(AviationMessageArchiver.class.getSimpleName());
    }

    // TODO: Remove this
    @Bean
    public String testBean(@Value("${test.value}") final String value) {
        System.out.println("Running module: " + value);
        return value;
    }

}
