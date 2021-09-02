package fi.fmi.avi.archiver.database;

import fi.fmi.avi.archiver.AviationMessageArchiver;
import fi.fmi.avi.archiver.TestConfig;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessageIWXXMDetails;
import fi.fmi.avi.archiver.message.ProcessingResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@JdbcTest(properties = {"testclass.name=fi.fmi.avi.archiver.database.DatabaseAccessTest"})
@Sql(scripts = {"classpath:/schema-h2.sql", "classpath:/h2-data/avidb_test_content.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:/h2-data/avidb_cleanup_test.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
@ContextConfiguration(classes = {AviationMessageArchiver.class, TestConfig.class},//
        loader = AnnotationConfigContextLoader.class,//
        initializers = {ConfigDataApplicationContextInitializer.class})
public class DatabaseAccessTest {

    private static final String SELECT_AVIATION_MESSAGES = "select message_time, station_id, type_id, " +
            "route_id, message, valid_from, valid_to, created, file_modified, flag, messir_heading, version, format_id, " +
            "avidb_message_iwxxm_details.collect_identifier, avidb_iwxxm_version.iwxxm_version " +
            "from avidb_messages " +
            "left join avidb_message_iwxxm_details " +
            "on avidb_messages.message_id = avidb_message_iwxxm_details.message_id " +
            "left join avidb_iwxxm_version " +
            "on avidb_message_iwxxm_details.iwxxm_version = avidb_iwxxm_version.version_id";
    private static final String SELECT_REJECTED_MESSAGES = "select icao_code, message_time, type_id, route_id, message, "
            + "valid_from, valid_to, created, file_modified, flag, messir_heading, reject_reason, version from avidb_rejected_messages";
    private static final Instant NOW = Instant.now();
    private static final String IWXXM_2_1_NAMESPACE = "http://icao.int/iwxxm/2.1";
    private static final ArchiveAviationMessage TEST_MESSAGE = ArchiveAviationMessage.builder()
            .setMessageTime(NOW)
            .setStationId(1)
            .setIcaoAirportCode("EFXX")
            .setFormat(1)
            .setType(2)
            .setRoute(1)
            .setMessage("TAF =")
            .setValidFrom(NOW)
            .setValidTo(NOW)
            .setFileModified(NOW)
            .setHeading("TEST HEADING")
            .build();

    @Autowired
    private DatabaseAccess databaseAccess;
    @Autowired
    private Clock clock;
    @SpyBean
    private JdbcTemplate jdbcTemplate;

    @Test
    public void test_insert_aviation_message() {
        final Number generatedId = databaseAccess.insertAviationMessage(TEST_MESSAGE);
        assertThat(generatedId.longValue()).isPositive();
        assertAvidbMessagesContains(TEST_MESSAGE);
    }

    @Test
    public void test_insert_aviation_message_with_nonexistent_station_id() {
        final ArchiveAviationMessage archiveAviationMessage = TEST_MESSAGE.toBuilder()
                .setStationId(200)
                .build();
        assertThrows(DataIntegrityViolationException.class, () -> databaseAccess.insertAviationMessage(archiveAviationMessage));
    }

    @Test
    public void test_insert_aviation_message_with_iwxxm_details() {
        final ArchiveAviationMessage archiveAviationMessage = TEST_MESSAGE.toBuilder()
                .setIWXXMDetails(ArchiveAviationMessageIWXXMDetails.builder()
                        .setCollectIdentifier("test identifier")
                        .setXMLNamespace(IWXXM_2_1_NAMESPACE)
                        .build())
                .build();

        final Number generatedId = databaseAccess.insertAviationMessage(archiveAviationMessage);
        assertThat(generatedId.longValue()).isPositive();
        assertAvidbMessagesContains(archiveAviationMessage);
    }

    @Test
    public void test_insert_aviation_message_with_new_iwxxm_version() {
        final ArchiveAviationMessage archiveAviationMessage = TEST_MESSAGE.toBuilder()
                .setIWXXMDetails(ArchiveAviationMessageIWXXMDetails.builder()
                        .setCollectIdentifier("test identifier")
                        .setXMLNamespace("http://test.namespace")
                        .build())
                .build();

        final Number generatedId = databaseAccess.insertAviationMessage(archiveAviationMessage);
        assertThat(generatedId.longValue()).isPositive();
        assertAvidbMessagesContains(archiveAviationMessage);
    }

    @Test
    public void test_insert_aviation_message_with_iwxxm_details_only_collect_identifier() {
        final ArchiveAviationMessage archiveAviationMessage = TEST_MESSAGE.toBuilder()
                .setIWXXMDetails(ArchiveAviationMessageIWXXMDetails.builder()
                        .setCollectIdentifier("test identifier")
                        .build())
                .build();

        final Number generatedId = databaseAccess.insertAviationMessage(archiveAviationMessage);
        assertThat(generatedId.longValue()).isPositive();
        assertAvidbMessagesContains(archiveAviationMessage);
    }

    @Test
    public void test_insert_aviation_message_with_iwxxm_details_only_version() {
        final ArchiveAviationMessage archiveAviationMessage = TEST_MESSAGE.toBuilder()
                .setIWXXMDetails(ArchiveAviationMessageIWXXMDetails.builder()
                        .setXMLNamespace(IWXXM_2_1_NAMESPACE)
                        .build())
                .build();

        final Number generatedId = databaseAccess.insertAviationMessage(archiveAviationMessage);
        assertThat(generatedId.longValue()).isPositive();
        assertAvidbMessagesContains(archiveAviationMessage);
    }

    @Test
    public void test_insert_aviation_message_with_query_timeouts_and_retries() {
        doThrow(QueryTimeoutException.class)
                .doThrow(QueryTimeoutException.class)
                .doCallRealMethod()
                .when(jdbcTemplate).update(any(PreparedStatementCreator.class), any(KeyHolder.class));

        final Number generatedId = databaseAccess.insertAviationMessage(TEST_MESSAGE);
        verify(jdbcTemplate, times(3)).update(any(PreparedStatementCreator.class), any(KeyHolder.class));
        assertThat(generatedId.longValue()).isPositive();
    }

    @Test
    public void test_insert_rejected_aviation_message() {
        final ArchiveAviationMessage archiveAviationMessage = TEST_MESSAGE.toBuilder()
                .setProcessingResult(ProcessingResult.UNKNOWN_ICAO_CODE)//
                .build();

        final int affectedRows = databaseAccess.insertRejectedAviationMessage(archiveAviationMessage);
        assertThat(affectedRows).isEqualTo(1);
        assertAvidbRejectedMessagesContains(archiveAviationMessage);
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

    @Test
    public void test_query_iwxxm_version() {
        final Optional<Integer> versionId = databaseAccess.queryOrInsertIwxxmVersion(IWXXM_2_1_NAMESPACE);
        assertThat(versionId).contains(1);
    }

    @Test
    public void test_insert_iwxxm_version() {
        final Optional<Integer> versionId = databaseAccess.queryOrInsertIwxxmVersion("test");
        assertThat(versionId).contains(2);
    }

    private void assertAvidbMessagesContains(final ArchiveAviationMessage archiveAviationMessage) {
        databaseAccess.getJdbcTemplate().queryForObject(SELECT_AVIATION_MESSAGES, Collections.emptyMap(), (RowMapper<ArchiveAviationMessage>) (rs, rowNum) -> {
            assertThat(rs.getObject(1, Instant.class)).isEqualTo(archiveAviationMessage.getMessageTime());
            assertThat(rs.getInt(2)).isEqualTo(archiveAviationMessage.getStationId().orElse(-1));
            assertThat(rs.getInt(3)).isEqualTo(archiveAviationMessage.getType());
            assertThat(rs.getInt(4)).isEqualTo(archiveAviationMessage.getRoute());
            assertThat(rs.getString(5)).isEqualTo(archiveAviationMessage.getMessage());
            assertThat(rs.getObject(6, Instant.class)).isEqualTo(archiveAviationMessage.getValidFrom().orElse(Instant.EPOCH));
            assertThat(rs.getObject(7, Instant.class)).isEqualTo(archiveAviationMessage.getValidTo().orElse(Instant.EPOCH));
            assertThat(rs.getObject(8, Instant.class)).isEqualTo(clock.instant());
            assertThat(rs.getObject(9, Instant.class)).isEqualTo(archiveAviationMessage.getFileModified().orElse(Instant.EPOCH));
            assertThat(rs.getObject(10)).isEqualTo(0);
            assertThat(rs.getString(11)).isEqualTo(archiveAviationMessage.getHeading().orElse(null));
            assertThat(rs.getString(12)).isNull();
            assertThat(rs.getInt(13)).isEqualTo(1);

            if (archiveAviationMessage.getIWXXMDetails().isEmpty()) {
                assertThat(rs.getObject(14)).isNull();
                assertThat(rs.getObject(15)).isNull();
            } else {
                assertThat(rs.getString(14)).isEqualTo(archiveAviationMessage.getIWXXMDetails().getCollectIdentifier().orElse(null));
                assertThat(rs.getString(15)).isEqualTo(archiveAviationMessage.getIWXXMDetails().getXMLNamespace().orElse(null));
            }
            return null;
        });
    }

    private void assertAvidbRejectedMessagesContains(final ArchiveAviationMessage archiveAviationMessage) {
        databaseAccess.getJdbcTemplate().queryForObject(SELECT_REJECTED_MESSAGES, Collections.emptyMap(), (RowMapper<ArchiveAviationMessage>) (rs, rowNum) -> {
            assertThat(rs.getString(1)).isEqualTo(archiveAviationMessage.getIcaoAirportCode());
            assertThat(rs.getObject(2, Instant.class)).isEqualTo(archiveAviationMessage.getMessageTime());
            assertThat(rs.getInt(3)).isEqualTo(archiveAviationMessage.getType());
            assertThat(rs.getInt(4)).isEqualTo(archiveAviationMessage.getRoute());
            assertThat(rs.getString(5)).isEqualTo(archiveAviationMessage.getMessage());
            assertThat(rs.getObject(6, Instant.class)).isEqualTo(archiveAviationMessage.getValidFrom().orElse(Instant.EPOCH));
            assertThat(rs.getObject(7, Instant.class)).isEqualTo(archiveAviationMessage.getValidTo().orElse(Instant.EPOCH));
            assertThat(rs.getObject(8, Instant.class)).isEqualTo(clock.instant());
            assertThat(rs.getObject(9, Instant.class)).isEqualTo(archiveAviationMessage.getFileModified().orElse(Instant.EPOCH));
            assertThat(rs.getObject(10)).isEqualTo(0);
            assertThat(rs.getString(11)).isEqualTo(archiveAviationMessage.getHeading().orElse(null));
            assertThat(rs.getInt(12)).isEqualTo(archiveAviationMessage.getProcessingResult().getCode());
            assertThat(rs.getString(13)).isNull();
            return null;
        });
    }

}
