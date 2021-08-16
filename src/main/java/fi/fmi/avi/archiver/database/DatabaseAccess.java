package fi.fmi.avi.archiver.database;

import static java.util.Objects.requireNonNull;

import java.time.Clock;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.retry.support.RetryTemplate;

import com.google.common.annotations.VisibleForTesting;

import fi.fmi.avi.archiver.message.ArchiveAviationMessage;

public class DatabaseAccess {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseAccess.class);
    private static final String STATION_ID_QUERY = "select station_id from avidb_stations where icao_code = :icao_code";

    private final Clock clock;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final RetryTemplate retryTemplate;
    private final SimpleJdbcInsert insertAviationMessage;
    private final SimpleJdbcInsert insertRejectedAviationMessage;

    public DatabaseAccess(final NamedParameterJdbcTemplate jdbcTemplate, final Clock clock, final RetryTemplate retryTemplate) {
        this.clock = requireNonNull(clock, "clock");
        this.jdbcTemplate = requireNonNull(jdbcTemplate, "jdbcTemplate");
        this.retryTemplate = requireNonNull(retryTemplate, "retryTemplate");
        this.insertAviationMessage = new SimpleJdbcInsert(jdbcTemplate.getJdbcTemplate()).withTableName("avidb_messages")
                .usingGeneratedKeyColumns("message_id");
        this.insertRejectedAviationMessage = new SimpleJdbcInsert(jdbcTemplate.getJdbcTemplate()).withTableName("avidb_rejected_messages");
    }

    @VisibleForTesting
    NamedParameterJdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

    public int insertAviationMessage(final ArchiveAviationMessage archiveAviationMessage) {
        final MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("message_time", archiveAviationMessage.getMessageTime());
        parameters.addValue("station_id", archiveAviationMessage.getStationId()
                .orElseThrow(() -> new IllegalArgumentException("Station id has to be supplied when inserting an aviation message")));
        parameters.addValue("type_id", archiveAviationMessage.getType());
        parameters.addValue("route_id", archiveAviationMessage.getRoute());
        parameters.addValue("message", archiveAviationMessage.getMessage());
        parameters.addValue("valid_from", archiveAviationMessage.getValidFrom().orElse(null));
        parameters.addValue("valid_to", archiveAviationMessage.getValidTo().orElse(null));
        parameters.addValue("created", clock.instant());
        parameters.addValue("file_modified", archiveAviationMessage.getFileModified().orElse(null));
        parameters.addValue("flag", 0);
        parameters.addValue("messir_heading", archiveAviationMessage.getHeading().orElse(null));
        parameters.addValue("version", archiveAviationMessage.getVersion().orElse(null));
        parameters.addValue("format_id", archiveAviationMessage.getFormat());
        // TODO: Store archiveAviationMessage.getIWXXMDetails() for IWXXM messages
        try {
            return retryTemplate.execute(context -> insertAviationMessage.execute(parameters));
        } catch (final RuntimeException e) {
            LOGGER.error("Inserting aviation message {} failed", archiveAviationMessage, e);
            throw e;
        }
    }

    public int insertRejectedAviationMessage(final ArchiveAviationMessage archiveAviationMessage) {
        final MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("icao_code", archiveAviationMessage.getIcaoAirportCode());
        parameters.addValue("message_time", archiveAviationMessage.getMessageTime());
        parameters.addValue("type_id", archiveAviationMessage.getType());
        parameters.addValue("route_id", archiveAviationMessage.getRoute());
        parameters.addValue("message", archiveAviationMessage.getMessage());
        parameters.addValue("valid_from", archiveAviationMessage.getValidFrom().orElse(null));
        parameters.addValue("valid_to", archiveAviationMessage.getValidTo().orElse(null));
        parameters.addValue("created", clock.instant());
        parameters.addValue("file_modified", archiveAviationMessage.getFileModified().orElse(null));
        parameters.addValue("flag", 0);
        parameters.addValue("messir_heading", archiveAviationMessage.getHeading().orElse(null));
        parameters.addValue("reject_reason", archiveAviationMessage.getProcessingResult().getCode());
        parameters.addValue("version", archiveAviationMessage.getVersion().orElse(null));
        // TODO: Store archiveAviationMessage.getIWXXMDetails() for IWXXM messages
        try {
            return retryTemplate.execute(context -> insertRejectedAviationMessage.execute(parameters));
        } catch (final RuntimeException e) {
            LOGGER.error("Inserting rejected aviation message {} failed", archiveAviationMessage, e);
            throw e;
        }
    }

    public Optional<Integer> queryStationId(final String icaoAirportCode) {
        final MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("icao_code", icaoAirportCode);
        try {
            final Integer stationId = retryTemplate.execute(context -> jdbcTemplate.queryForObject(STATION_ID_QUERY, parameters, Integer.class));
            return Optional.ofNullable(stationId);
        } catch (final EmptyResultDataAccessException ignored) {
            // No station was found
        } catch (final RuntimeException e) {
            LOGGER.error("Querying station id with icao airport code {} failed", icaoAirportCode, e);
        }
        return Optional.empty();
    }

}
