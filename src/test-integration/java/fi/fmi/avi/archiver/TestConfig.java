package fi.fmi.avi.archiver;

import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

@Configuration
public class TestConfig {

    @Bean
    public Clock clock() {
        return Clock.fixed(Instant.parse("2019-05-01T00:00:00Z"), ZoneId.of("UTC"));
    }

    @Bean
    public ApplicationConversionService conversionService() {
        return new ApplicationConversionService();
    }

}
