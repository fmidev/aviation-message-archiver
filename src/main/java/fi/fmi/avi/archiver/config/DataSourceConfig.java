package fi.fmi.avi.archiver.config;

import fi.fmi.avi.archiver.database.DatabaseAccess;
import fi.fmi.avi.archiver.database.DatabaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.messaging.MessageChannel;

import java.time.Clock;

@Configuration
public class DataSourceConfig {

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    private Clock clock;

    @Autowired
    private MessageChannel archiveChannel;

    @Bean
    public DatabaseAccess databaseAccess() {
        return new DatabaseAccess(jdbcTemplate, clock);
    }

    @Bean
    public DatabaseService databaseService() {
        return new DatabaseService(databaseAccess());
    }

    @Bean
    public IntegrationFlow databaseFlow() {
        return IntegrationFlows.from(archiveChannel)
                .handle(databaseService())//
                .get();
    }

}
