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

import java.time.Clock;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public class DatabaseAccess {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseAccess.class);
    private static final String STATION_ID_QUERY = "select station_id from avidb_stations where icao_code = :icao_code";

    private final Clock clock;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final RetryTemplate retryTemplate;
    private final SimpleJdbcInsert insertAviationMessage;
    private final SimpleJdbcInsert insertRejectedAviationMessage;
    private final SimpleJdbcInsert insertIwxxmDetails;

    public DatabaseAccess(final NamedParameterJdbcTemplate jdbcTemplate, final Clock clock, final RetryTemplate retryTemplate) {
        this.clock = requireNonNull(clock, "clock");
        this.jdbcTemplate = requireNonNull(jdbcTemplate, "jdbcTemplate");
        this.retryTemplate = requireNonNull(retryTemplate, "retryTemplate");

        this.insertAviationMessage = new SimpleJdbcInsert(jdbcTemplate.getJdbcTemplate()).withTableName("avidb_messages")
                .usingGeneratedKeyColumns("message_id");
        this.insertRejectedAviationMessage = new SimpleJdbcInsert(jdbcTemplate.getJdbcTemplate()).withTableName("avidb_rejected_messages");
        this.insertIwxxmDetails = new SimpleJdbcInsert(jdbcTemplate.getJdbcTemplate()).withTableName("avidb_message_iwxxm_details");
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
        parameters.addValue("message_time", archiveAviationMessage.getMessageTime())
                .addValue("station_id", archiveAviationMessage.getStationId()
                        .orElseThrow(() -> new IllegalArgumentException("Station id has to be supplied when inserting an aviation message")))
                .addValue("type_id", archiveAviationMessage.getType())
                .addValue("route_id", archiveAviationMessage.getRoute())
                .addValue("message", archiveAviationMessage.getMessage())
                .addValue("valid_from", archiveAviationMessage.getValidFrom().orElse(null))
                .addValue("valid_to", archiveAviationMessage.getValidTo().orElse(null))
                .addValue("created", clock.instant())
                .addValue("file_modified", archiveAviationMessage.getFileModified().orElse(null))
                .addValue("flag", 0)
                .addValue("messir_heading", archiveAviationMessage.getHeading().orElse(null))
                .addValue("version", archiveAviationMessage.getVersion().orElse(null))
                .addValue("format_id", archiveAviationMessage.getFormat());
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
     * Insert aviation message into the rejected messages table. Returns the affected row count.
     *
     * @param archiveAviationMessage aviation message to archive in the rejected messages table
     * @return affected row count
     */
    public int insertRejectedAviationMessage(final ArchiveAviationMessage archiveAviationMessage) {
        final MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("icao_code", archiveAviationMessage.getStationIcaoCode())
                .addValue("message_time", archiveAviationMessage.getMessageTime())
                .addValue("type_id", archiveAviationMessage.getType())
                .addValue("route_id", archiveAviationMessage.getRoute())
                .addValue("message", archiveAviationMessage.getMessage())
                .addValue("valid_from", archiveAviationMessage.getValidFrom().orElse(null))
                .addValue("valid_to", archiveAviationMessage.getValidTo().orElse(null))
                .addValue("created", clock.instant())
                .addValue("file_modified", archiveAviationMessage.getFileModified().orElse(null))
                .addValue("flag", 0)
                .addValue("messir_heading", archiveAviationMessage.getHeading().orElse(null))
                .addValue("reject_reason", archiveAviationMessage.getProcessingResult().getCode())
                .addValue("version", archiveAviationMessage.getVersion().orElse(null));
        try {
            return retryTemplate.execute(context -> insertRejectedAviationMessage.execute(parameters));
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

    public Optional<Integer> queryStationId(final String icaoAirportCode) {
        requireNonNull(icaoAirportCode, "icaoAirportCode");
        final MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("icao_code", icaoAirportCode);
        try {
            final Integer stationId = retryTemplate.execute(context ->
                    jdbcTemplate.queryForObject(STATION_ID_QUERY, parameters, Integer.class));
            return Optional.ofNullable(stationId);
        } catch (final EmptyResultDataAccessException ignored) {
            // No station was found
        } catch (final RuntimeException e) {
            LOGGER.error("Querying station id with icao airport code {} failed", icaoAirportCode, e);
        }
        return Optional.empty();
    }

}
