package fi.fmi.avi.archiver.database;

import fi.fmi.avi.archiver.AviationMessageArchiver;
import fi.fmi.avi.archiver.TestConfig;
import fi.fmi.avi.archiver.message.AviationMessage;
import fi.fmi.avi.archiver.message.ProcessingResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@Sql(scripts = {"classpath:/schema-h2.sql", "classpath:/h2-data/avidb_message_types_test.sql",
        "classpath:/h2-data/avidb_message_format_test.sql", "classpath:/h2-data/avidb_message_routes_test.sql",
        "classpath:/h2-data/avidb_stations_test.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:/h2-data/avidb_cleanup_test.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
@ContextConfiguration(classes = {AviationMessageArchiver.class, TestConfig.class},//
        loader = AnnotationConfigContextLoader.class,//
        initializers = {ConfigDataApplicationContextInitializer.class})
public class DatabaseAccessTest {

    private static final String SELECT_AVIATION_MESSAGES = "select message_time, station_id, type_id, route_id, message," +
            " valid_from, valid_to, created, file_modified, flag, messir_heading, version, format_id from avidb_messages";
    private static final String SELECT_REJECTED_MESSAGES = "select icao_code, message_time, type_id, route_id, message, " +
            "valid_from, valid_to, created, file_modified, flag, messir_heading, reject_reason, version from avidb_rejected_messages";

    @Autowired
    private DatabaseAccess databaseAccess;

    @Autowired
    private Clock clock;

    @Test
    public void test_insert_aviation_message() {
        final Instant now = clock.instant();
        final AviationMessage aviationMessage = AviationMessage.builder()//
                .setMessageTime(now)//
                .setStationId(1)
                .setIcaoAirportCode("EFXX")//
                .setType(2)//
                .setRoute(1)//
                .setMessage("TAF =")//
                .setValidFrom(now)//
                .setValidTo(now)//
                .setFileModified(now)//
                .setHeading("TEST HEADING")//
                .build();

        final int affectedRows = databaseAccess.insertAviationMessage(aviationMessage);
        assertThat(affectedRows).isEqualTo(1);
        assertAvidbMessagesContains(aviationMessage);
    }

    @Test
    public void test_insert_rejected_aviation_message() {
        final Instant now = clock.instant();
        final AviationMessage aviationMessage = AviationMessage.builder()//
                .setMessageTime(now)//
                .setIcaoAirportCode("XXXX")//
                .setType(2)//
                .setRoute(1)//
                .setMessage("TAF =")//
                .setValidFrom(now)//
                .setValidTo(now)//
                .setFileModified(now)//
                .setHeading("TEST HEADING")//
                .setProcessingResult(ProcessingResult.UNKNOWN_ICAO_CODE)//
                .build();

        final int affectedRows = databaseAccess.insertRejectedAviationMessage(aviationMessage);
        assertThat(affectedRows).isEqualTo(1);
        assertAvidbRejectedMessagesContains(aviationMessage);
    }

    @Test
    public void test_query_existing_station() {
        final Optional<Integer> testId = databaseAccess.queryStationId("EFXX");
        assertThat(testId).isPresent();
    }

    @Test
    public void test_query_nonexistent_station() {
        final Optional<Integer> testId = databaseAccess.queryStationId("XXXX");
        assertThat(testId).isEmpty();
    }

    private void assertAvidbMessagesContains(final AviationMessage aviationMessage) {
        databaseAccess.getJdbcTemplate().queryForObject(SELECT_AVIATION_MESSAGES, Collections.emptyMap(), (RowMapper<AviationMessage>) (rs, rowNum) -> {
            assertThat(rs.getObject(1, Instant.class)).isEqualTo(aviationMessage.getMessageTime());
            assertThat(rs.getInt(2)).isEqualTo(aviationMessage.getStationId().orElse(-1));
            assertThat(rs.getInt(3)).isEqualTo(aviationMessage.getType());
            assertThat(rs.getInt(4)).isEqualTo(aviationMessage.getRoute());
            assertThat(rs.getString(5)).isEqualTo(aviationMessage.getMessage());
            assertThat(rs.getObject(6, Instant.class)).isEqualTo(aviationMessage.getValidFrom().orElse(Instant.EPOCH));
            assertThat(rs.getObject(7, Instant.class)).isEqualTo(aviationMessage.getValidTo().orElse(Instant.EPOCH));
            assertThat(rs.getObject(8, Instant.class)).isEqualTo(clock.instant());
            assertThat(rs.getObject(9, Instant.class)).isEqualTo(aviationMessage.getFileModified().orElse(Instant.EPOCH));
            assertThat(rs.getObject(10)).isEqualTo(0);
            assertThat(rs.getString(11)).isEqualTo(aviationMessage.getHeading());
            assertThat(rs.getString(12)).isNull();
            assertThat(rs.getInt(13)).isEqualTo(1);
            return null;
        });
    }

    private void assertAvidbRejectedMessagesContains(final AviationMessage aviationMessage) {
        databaseAccess.getJdbcTemplate().queryForObject(SELECT_REJECTED_MESSAGES, Collections.emptyMap(), (RowMapper<AviationMessage>) (rs, rowNum) -> {
            assertThat(rs.getString(1)).isEqualTo(aviationMessage.getIcaoAirportCode());
            assertThat(rs.getObject(2, Instant.class)).isEqualTo(aviationMessage.getMessageTime());
            assertThat(rs.getInt(3)).isEqualTo(aviationMessage.getType());
            assertThat(rs.getInt(4)).isEqualTo(aviationMessage.getRoute());
            assertThat(rs.getString(5)).isEqualTo(aviationMessage.getMessage());
            assertThat(rs.getObject(6, Instant.class)).isEqualTo(aviationMessage.getValidFrom().orElse(Instant.EPOCH));
            assertThat(rs.getObject(7, Instant.class)).isEqualTo(aviationMessage.getValidTo().orElse(Instant.EPOCH));
            assertThat(rs.getObject(8, Instant.class)).isEqualTo(clock.instant());
            assertThat(rs.getObject(9, Instant.class)).isEqualTo(aviationMessage.getFileModified().orElse(Instant.EPOCH));
            assertThat(rs.getObject(10)).isEqualTo(0);
            assertThat(rs.getString(11)).isEqualTo(aviationMessage.getHeading());
            assertThat(rs.getInt(12)).isEqualTo(aviationMessage.getProcessingResult().getCode());
            assertThat(rs.getString(13)).isNull();
            return null;
        });
    }

}
