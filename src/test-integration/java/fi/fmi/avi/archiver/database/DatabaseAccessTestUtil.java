package fi.fmi.avi.archiver.database;

import fi.fmi.avi.archiver.message.ArchivalStatus;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessageIWXXMDetails;
import fi.fmi.avi.archiver.message.ProcessingResult;
import org.springframework.jdbc.core.RowMapper;

import javax.annotation.Nullable;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

public class DatabaseAccessTestUtil {

    private static final String SELECT_AVIATION_MESSAGES = "select message_time, station_id, type_id, "
            + "route_id, message, valid_from, valid_to, created, file_modified, flag, messir_heading, version, format_id, "
            + "collect_identifier, iwxxm_version from avidb_messages left join avidb_message_iwxxm_details "
            + "on avidb_messages.message_id = avidb_message_iwxxm_details.message_id";
    private static final String SELECT_AVIATION_MESSAGES_WITH_STATION_ICAO_CODE = "select message_time, "
            + "avidb_messages.station_id, type_id, route_id, message, avidb_messages.valid_from, avidb_messages.valid_to, "
            + "created, file_modified, flag, messir_heading, version, format_id, collect_identifier, iwxxm_version, "
            + "icao_code from avidb_messages left join avidb_message_iwxxm_details on "
            + "avidb_messages.message_id = avidb_message_iwxxm_details.message_id left join avidb_stations on "
            + "avidb_messages.station_id = avidb_stations.station_id";
    private static final String SELECT_REJECTED_MESSAGES = "select icao_code, message_time, type_id, route_id, message, "
            + "valid_from, valid_to, created, file_modified, flag, messir_heading, reject_reason, version, format_id, "
            + "collect_identifier, iwxxm_version from avidb_rejected_messages left join avidb_rejected_message_iwxxm_details on "
            + "avidb_rejected_messages.rejected_message_id = avidb_rejected_message_iwxxm_details.rejected_message_id";
    private static final String COUNT_MESSAGES = "select count(*) from avidb_messages";
    private static final String COUNT_REJECTED_MESSAGES = "select count(*) from avidb_rejected_messages";

    private final DatabaseAccess databaseAccess;
    private final Instant creationTimestamp;

    public DatabaseAccessTestUtil(final DatabaseAccess databaseAccess, final Instant creationTimestamp) {
        this.databaseAccess = requireNonNull(databaseAccess, "databaseAccess");
        this.creationTimestamp = requireNonNull(creationTimestamp, "creationTimestamp");
    }

    public void assertMessagesContains(final ArchiveAviationMessage archiveAviationMessage) {
        databaseAccess.getJdbcTemplate().queryForObject(SELECT_AVIATION_MESSAGES, Collections.emptyMap(), (RowMapper<ArchiveAviationMessage>) (rs, rowNum) -> {
            assertThat(toInstant(rs.getObject(1, OffsetDateTime.class))).isEqualTo(archiveAviationMessage.getMessageTime());
            assertThat(rs.getInt(2)).isEqualTo(archiveAviationMessage.getStationId().orElse(-1));
            assertThat(rs.getInt(3)).isEqualTo(archiveAviationMessage.getType());
            assertThat(rs.getInt(4)).isEqualTo(archiveAviationMessage.getRoute());
            assertThat(rs.getString(5)).isEqualTo(archiveAviationMessage.getMessage());
            assertThat(toInstant(rs.getObject(6, OffsetDateTime.class))).isEqualTo(archiveAviationMessage.getValidFrom().orElse(Instant.EPOCH));
            assertThat(toInstant(rs.getObject(7, OffsetDateTime.class))).isEqualTo(archiveAviationMessage.getValidTo().orElse(Instant.EPOCH));
            assertThat(toInstant(rs.getObject(8, OffsetDateTime.class))).isEqualTo(creationTimestamp);
            assertThat(toInstant(rs.getObject(9, OffsetDateTime.class))).isEqualTo(archiveAviationMessage.getFileModified().orElse(Instant.EPOCH));
            assertThat(rs.getObject(10)).isEqualTo(0);
            assertThat(rs.getString(11)).isEqualTo(archiveAviationMessage.getHeading().orElse(null));
            assertThat(rs.getString(12)).isNull();
            assertThat(rs.getInt(13)).isEqualTo(archiveAviationMessage.getFormat());

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

    public void assertRejectedMessagesContains(final ArchiveAviationMessage archiveAviationMessage) {
        databaseAccess.getJdbcTemplate().queryForObject(SELECT_REJECTED_MESSAGES, Collections.emptyMap(), (RowMapper<ArchiveAviationMessage>) (rs, rowNum) -> {
            assertThat(rs.getString(1)).isEqualTo(archiveAviationMessage.getStationIcaoCode());
            assertThat(toInstant(rs.getObject(2, OffsetDateTime.class))).isEqualTo(archiveAviationMessage.getMessageTime());
            assertThat(rs.getInt(3)).isEqualTo(archiveAviationMessage.getType());
            assertThat(rs.getInt(4)).isEqualTo(archiveAviationMessage.getRoute());
            assertThat(rs.getString(5)).isEqualTo(archiveAviationMessage.getMessage());
            assertThat(toInstant(rs.getObject(6, OffsetDateTime.class))).isEqualTo(archiveAviationMessage.getValidFrom().orElse(Instant.EPOCH));
            assertThat(toInstant(rs.getObject(7, OffsetDateTime.class))).isEqualTo(archiveAviationMessage.getValidTo().orElse(Instant.EPOCH));
            assertThat(toInstant(rs.getObject(8, OffsetDateTime.class))).isEqualTo(creationTimestamp);
            assertThat(toInstant(rs.getObject(9, OffsetDateTime.class))).isEqualTo(archiveAviationMessage.getFileModified().orElse(Instant.EPOCH));
            assertThat(rs.getObject(10)).isEqualTo(0);
            assertThat(rs.getString(11)).isEqualTo(archiveAviationMessage.getHeading().orElse(null));
            assertThat(rs.getInt(12)).isEqualTo(archiveAviationMessage.getProcessingResult().getCode());
            assertThat(rs.getString(13)).isNull();
            assertThat(rs.getInt(14)).isEqualTo(archiveAviationMessage.getFormat());

            if (archiveAviationMessage.getIWXXMDetails().isEmpty()) {
                assertThat(rs.getObject(15)).isNull();
                assertThat(rs.getObject(16)).isNull();
            } else {
                assertThat(rs.getString(15)).isEqualTo(archiveAviationMessage.getIWXXMDetails().getCollectIdentifier().orElse(null));
                assertThat(rs.getString(16)).isEqualTo(archiveAviationMessage.getIWXXMDetails().getXMLNamespace().orElse(null));
            }
            return null;
        });
    }

    public List<ArchiveAviationMessage> fetchArchiveMessages() {
        return databaseAccess.getJdbcTemplate()
                .query(SELECT_AVIATION_MESSAGES_WITH_STATION_ICAO_CODE, Collections.emptyMap(), (rs, rowNum) ->
                        ArchiveAviationMessage.builder()
                                .setArchivalStatus(ArchivalStatus.ARCHIVED)
                                .setMessageTime(toInstant(rs.getObject(1, OffsetDateTime.class)))
                                .setStationId(rs.getInt(2))
                                .setType(rs.getInt(3))
                                .setRoute(rs.getInt(4))
                                .setMessage(rs.getString(5))
                                .setNullableValidFrom(toInstant(rs.getObject(6, OffsetDateTime.class)))
                                .setNullableValidTo(toInstant(rs.getObject(7, OffsetDateTime.class)))
                                .setNullableFileModified(toInstant(rs.getObject(9, OffsetDateTime.class)))
                                .setNullableHeading(rs.getString(11))
                                .setFormat(rs.getInt(13))
                                .setIWXXMDetails(ArchiveAviationMessageIWXXMDetails.builder()
                                        .setNullableCollectIdentifier(rs.getString(14))
                                        .setNullableXMLNamespace(rs.getString(15))
                                        .build())
                                .setStationIcaoCode(rs.getString(16))
                                .build());
    }

    public List<ArchiveAviationMessage> fetchRejectedMessages() {
        return databaseAccess.getJdbcTemplate()
                .query(SELECT_REJECTED_MESSAGES, Collections.emptyMap(), (rs, rowNum) ->
                        ArchiveAviationMessage.builder()
                                .setArchivalStatus(ArchivalStatus.REJECTED)
                                .setStationIcaoCode(rs.getString(1))
                                .setMessageTime(toInstant(rs.getObject(2, OffsetDateTime.class)))
                                .setType(rs.getInt(3))
                                .setRoute(rs.getInt(4))
                                .setMessage(rs.getString(5))
                                .setNullableValidFrom(toInstant(rs.getObject(6, OffsetDateTime.class)))
                                .setNullableValidTo(toInstant(rs.getObject(7, OffsetDateTime.class)))
                                .setNullableFileModified(toInstant(rs.getObject(9, OffsetDateTime.class)))
                                .setNullableHeading(rs.getString(11))
                                .setProcessingResult(ProcessingResult.values()[rs.getInt(12)])
                                .setFormat(rs.getInt(14))
                                .setIWXXMDetails(ArchiveAviationMessageIWXXMDetails.builder()
                                        .setNullableCollectIdentifier(rs.getString(15))
                                        .setNullableXMLNamespace(rs.getString(16))
                                        .build())
                                .build());
    }

    public void assertMessagesEmpty() {
        final Integer count = databaseAccess.getJdbcTemplate().queryForObject(COUNT_MESSAGES, Collections.emptyMap(), Integer.class);
        assertThat(count).isZero();
    }

    public void assertRejectedMessagesEmpty() {
        final Integer count = databaseAccess.getJdbcTemplate().queryForObject(COUNT_REJECTED_MESSAGES, Collections.emptyMap(), Integer.class);
        assertThat(count).isZero();
    }

    @Nullable
    public Instant toInstant(@Nullable final OffsetDateTime offsetDateTime) {
        return offsetDateTime == null ? null : offsetDateTime.toInstant();
    }

}
