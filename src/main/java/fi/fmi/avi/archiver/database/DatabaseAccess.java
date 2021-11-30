package fi.fmi.avi.archiver.database;

import com.google.common.annotations.VisibleForTesting;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessageIWXXMDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.retry.support.RetryTemplate;

import javax.annotation.Nullable;
import java.sql.Types;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public class DatabaseAccess {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseAccess.class);

    private final Clock clock;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final RetryTemplate retryTemplate;
    private final SimpleJdbcInsert insertAviationMessage;
    private final SimpleJdbcInsert insertRejectedAviationMessage;
    private final SimpleJdbcInsert insertIwxxmDetails;
    private final SimpleJdbcInsert insertRejectedIwxxmDetails;
    private final String stationIdQuery;

    public DatabaseAccess(final NamedParameterJdbcTemplate jdbcTemplate, final Clock clock, final RetryTemplate retryTemplate, final String schema) {
        this.clock = requireNonNull(clock, "clock");
        this.jdbcTemplate = requireNonNull(jdbcTemplate, "jdbcTemplate");
        this.retryTemplate = requireNonNull(retryTemplate, "retryTemplate");
        requireNonNull(schema, "schema");

        this.insertAviationMessage = new SimpleJdbcInsert(jdbcTemplate.getJdbcTemplate())
                .withSchemaName(schema)
                .withTableName("avidb_messages")
                .usingGeneratedKeyColumns("message_id");
        this.insertRejectedAviationMessage = new SimpleJdbcInsert(jdbcTemplate.getJdbcTemplate())
                .withSchemaName(schema)
                .withTableName("avidb_rejected_messages")
                .usingGeneratedKeyColumns("rejected_message_id");
        this.insertIwxxmDetails = new SimpleJdbcInsert(jdbcTemplate.getJdbcTemplate())
                .withSchemaName(schema)
                .withTableName("avidb_message_iwxxm_details");
        this.insertRejectedIwxxmDetails = new SimpleJdbcInsert(jdbcTemplate.getJdbcTemplate())
                .withSchemaName(schema)
                .withTableName("avidb_rejected_message_iwxxm_details");
        this.stationIdQuery = "select station_id from " + schema + ".avidb_stations where icao_code = :icao_code";
    }

    @VisibleForTesting
    NamedParameterJdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

    /**
     * Insert aviation message into the main message table. Returns the generated id.
     *
     * @param archiveAviationMessage aviation message to archive
     * @return the generated id
     */
    public Number insertAviationMessage(final ArchiveAviationMessage archiveAviationMessage) {
        final MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("station_id", archiveAviationMessage.getStationId()
                .orElseThrow(() -> new IllegalArgumentException("Station id has to be supplied when inserting an aviation message")));
        addCommonParameters(parameters, archiveAviationMessage);
        try {
            final Number id = retryTemplate.execute(context -> insertAviationMessage.executeAndReturnKey(parameters));
            if (!archiveAviationMessage.getIWXXMDetails().isEmpty()) {
                insertIwxxmDetails(id, archiveAviationMessage.getIWXXMDetails());
            }
            return id;
        } catch (final RuntimeException e) {
            LOGGER.error("Inserting aviation message {} failed", archiveAviationMessage, e);
            throw e;
        }
    }

    /**
     * Insert aviation message into the rejected messages table. Returns the generated id.
     *
     * @param archiveAviationMessage aviation message to archive in the rejected messages table
     * @return the generated id
     */
    public Number insertRejectedAviationMessage(final ArchiveAviationMessage archiveAviationMessage) {
        final MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("icao_code", archiveAviationMessage.getStationIcaoCode())
                .addValue("reject_reason", archiveAviationMessage.getProcessingResult().getCode());
        addCommonParameters(parameters, archiveAviationMessage);
        try {
            final Number id = retryTemplate.execute(context -> insertRejectedAviationMessage.executeAndReturnKey(parameters));
            if (!archiveAviationMessage.getIWXXMDetails().isEmpty()) {
                insertRejectedIwxxmDetails(id, archiveAviationMessage.getIWXXMDetails());
            }
            return id;
        } catch (final RuntimeException e) {
            LOGGER.error("Inserting rejected aviation message {} failed", archiveAviationMessage, e);
            throw e;
        }
    }

    private int insertIwxxmDetails(final Number messageId, final ArchiveAviationMessageIWXXMDetails iwxxmDetails) {
        final MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("message_id", messageId)
                .addValue("collect_identifier", iwxxmDetails.getCollectIdentifier().orElse(null))
                .addValue("iwxxm_version", iwxxmDetails.getXMLNamespace().orElse(null));
        try {
            return retryTemplate.execute(context -> insertIwxxmDetails.execute(parameters));
        } catch (final RuntimeException e) {
            LOGGER.error("Inserting aviation message IWXXM details failed for message id {}", messageId, e);
            throw e;
        }
    }

    private int insertRejectedIwxxmDetails(final Number rejectedMessageId, final ArchiveAviationMessageIWXXMDetails iwxxmDetails) {
        final MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("rejected_message_id", rejectedMessageId)
                .addValue("collect_identifier", iwxxmDetails.getCollectIdentifier().orElse(null))
                .addValue("iwxxm_version", iwxxmDetails.getXMLNamespace().orElse(null));
        try {
            return retryTemplate.execute(context -> insertRejectedIwxxmDetails.execute(parameters));
        } catch (final RuntimeException e) {
            LOGGER.error("Inserting rejected message IWXXM details failed for rejected message id {}", rejectedMessageId, e);
            throw e;
        }
    }

    public Optional<Integer> queryStationId(final String stationIcaoCode) {
        requireNonNull(stationIcaoCode, "stationIcaoCode");
        final MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("icao_code", stationIcaoCode);
        try {
            final Integer stationId = retryTemplate.execute(context -> jdbcTemplate.queryForObject(stationIdQuery, parameters, Integer.class));
            return Optional.ofNullable(stationId);
        } catch (final EmptyResultDataAccessException ignored) {
            // No station was found
        } catch (final RuntimeException e) {
            LOGGER.error("Querying station id with icao airport code {} failed", stationIcaoCode, e);
        }
        return Optional.empty();
    }

    private void addCommonParameters(final MapSqlParameterSource parameters, final ArchiveAviationMessage archiveAviationMessage) {
        parameters.addValue("type_id", archiveAviationMessage.getType())
                .addValue("route_id", archiveAviationMessage.getRoute())
                .addValue("message", archiveAviationMessage.getMessage())
                .addValue("flag", 0)
                .addValue("messir_heading", archiveAviationMessage.getHeading().orElse(null))
                .addValue("version", archiveAviationMessage.getVersion().orElse(null))
                .addValue("format_id", archiveAviationMessage.getFormat());
        addTimestampWithTimezone(parameters, "message_time", archiveAviationMessage.getMessageTime());
        addTimestampWithTimezone(parameters, "valid_from", archiveAviationMessage.getValidFrom().orElse(null));
        addTimestampWithTimezone(parameters, "valid_to", archiveAviationMessage.getValidTo().orElse(null));
        addTimestampWithTimezone(parameters, "created", clock.instant());
        addTimestampWithTimezone(parameters, "file_modified", archiveAviationMessage.getFileModified().orElse(null));
    }

    private static void addTimestampWithTimezone(final MapSqlParameterSource parameters, final String name,
                                                 @Nullable final Instant timestamp) {
        parameters.addValue(name, timestamp == null ? null : timestamp.atOffset(ZoneOffset.UTC), Types.TIMESTAMP_WITH_TIMEZONE);
    }

}
