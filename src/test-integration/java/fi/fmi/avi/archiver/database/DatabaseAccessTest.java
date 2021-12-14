package fi.fmi.avi.archiver.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import fi.fmi.avi.archiver.AviationMessageArchiver;
import fi.fmi.avi.archiver.TestConfig;
import fi.fmi.avi.archiver.config.ConversionConfig;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessageIWXXMDetails;
import fi.fmi.avi.archiver.message.ProcessingResult;

@SpringBootTest({ "auto.startup=false", "testclass.name=fi.fmi.avi.archiver.database.DatabaseAccessTest" })
@Sql(scripts = { "classpath:/schema-h2.sql", "classpath:/h2-data/avidb_test_content.sql" }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:/h2-data/avidb_cleanup_test.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
@ContextConfiguration(classes = { AviationMessageArchiver.class, TestConfig.class, ConversionConfig.class },//
        loader = AnnotationConfigContextLoader.class,//
        initializers = { ConfigDataApplicationContextInitializer.class })
public class DatabaseAccessTest {

    private static final Instant NOW = Instant.now();
    private static final String IWXXM_2_1_NAMESPACE = "http://icao.int/iwxxm/2.1";
    private static final ArchiveAviationMessage TEST_MESSAGE = ArchiveAviationMessage.builder()
            .setMessageTime(NOW)
            .setStationId(1)
            .setStationIcaoCode("EFXX")
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

    private DatabaseAccessTestUtil databaseAccessTestUtil;

    @BeforeEach
    public void setUp() {
        databaseAccessTestUtil = new DatabaseAccessTestUtil(databaseAccess, clock.instant());
    }

    @Test
    public void test_empty_tables() {
        databaseAccessTestUtil.assertMessagesEmpty();
        databaseAccessTestUtil.assertRejectedMessagesEmpty();
    }

    @Test
    public void test_insert_aviation_message() {
        final Number generatedId = databaseAccess.insertAviationMessage(TEST_MESSAGE);
        assertThat(generatedId.longValue()).isPositive();
        databaseAccessTestUtil.assertMessagesContains(TEST_MESSAGE);
    }

    @Test
    public void test_insert_aviation_message_with_nonexistent_station_id() {
        final ArchiveAviationMessage archiveAviationMessage = TEST_MESSAGE.toBuilder().setStationId(200).build();
        assertThrows(DataIntegrityViolationException.class, () -> databaseAccess.insertAviationMessage(archiveAviationMessage));
    }

    @Test
    public void test_insert_aviation_message_with_iwxxm_details() {
        final ArchiveAviationMessage archiveAviationMessage = TEST_MESSAGE.toBuilder()
                .setIWXXMDetails(
                        ArchiveAviationMessageIWXXMDetails.builder().setCollectIdentifier("test identifier").setXMLNamespace(IWXXM_2_1_NAMESPACE).build())
                .build();

        final Number generatedId = databaseAccess.insertAviationMessage(archiveAviationMessage);
        assertThat(generatedId.longValue()).isPositive();
        databaseAccessTestUtil.assertMessagesContains(archiveAviationMessage);
    }

    @Test
    public void test_insert_aviation_message_with_new_iwxxm_version() {
        final ArchiveAviationMessage archiveAviationMessage = TEST_MESSAGE.toBuilder()
                .setIWXXMDetails(
                        ArchiveAviationMessageIWXXMDetails.builder().setCollectIdentifier("test identifier").setXMLNamespace("http://test.namespace").build())
                .build();

        final Number generatedId = databaseAccess.insertAviationMessage(archiveAviationMessage);
        assertThat(generatedId.longValue()).isPositive();
        databaseAccessTestUtil.assertMessagesContains(archiveAviationMessage);
    }

    @Test
    public void test_insert_aviation_message_with_iwxxm_details_only_collect_identifier() {
        final ArchiveAviationMessage archiveAviationMessage = TEST_MESSAGE.toBuilder()
                .setIWXXMDetails(ArchiveAviationMessageIWXXMDetails.builder().setCollectIdentifier("test identifier").build())
                .build();

        final Number generatedId = databaseAccess.insertAviationMessage(archiveAviationMessage);
        assertThat(generatedId.longValue()).isPositive();
        databaseAccessTestUtil.assertMessagesContains(archiveAviationMessage);
    }

    @Test
    public void test_insert_aviation_message_with_iwxxm_details_only_version() {
        final ArchiveAviationMessage archiveAviationMessage = TEST_MESSAGE.toBuilder()
                .setIWXXMDetails(ArchiveAviationMessageIWXXMDetails.builder().setXMLNamespace(IWXXM_2_1_NAMESPACE).build())
                .build();

        final Number generatedId = databaseAccess.insertAviationMessage(archiveAviationMessage);
        assertThat(generatedId.longValue()).isPositive();
        databaseAccessTestUtil.assertMessagesContains(archiveAviationMessage);
    }

    @Test
    public void test_insert_aviation_message_with_query_timeouts_and_retries() {
        doThrow(QueryTimeoutException.class).doThrow(QueryTimeoutException.class)
                .doCallRealMethod()
                .when(jdbcTemplate)
                .update(any(PreparedStatementCreator.class), any(KeyHolder.class));

        final Number generatedId = databaseAccess.insertAviationMessage(TEST_MESSAGE);
        verify(jdbcTemplate, times(3)).update(any(PreparedStatementCreator.class), any(KeyHolder.class));
        assertThat(generatedId.longValue()).isPositive();
    }

    @Test
    public void test_insert_rejected_aviation_message() {
        final ArchiveAviationMessage archiveAviationMessage = TEST_MESSAGE.toBuilder().setProcessingResult(ProcessingResult.UNKNOWN_STATION_ICAO_CODE)//
                .build();

        final Number generatedId = databaseAccess.insertRejectedAviationMessage(archiveAviationMessage);
        assertThat(generatedId.longValue()).isPositive();
        databaseAccessTestUtil.assertRejectedMessagesContains(archiveAviationMessage);
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

}
