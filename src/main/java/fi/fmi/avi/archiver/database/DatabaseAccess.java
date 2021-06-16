package fi.fmi.avi.archiver.database;

import com.google.common.annotations.VisibleForTesting;
import fi.fmi.avi.archiver.message.AviationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.retry.RetryCallback;
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

    public DatabaseAccess(final NamedParameterJdbcTemplate jdbcTemplate, final Clock clock,
                          final RetryTemplate retryTemplate) {
        this.clock = requireNonNull(clock, "clock");
        this.jdbcTemplate = requireNonNull(jdbcTemplate, "jdbcTemplate");
        this.retryTemplate = requireNonNull(retryTemplate, "retryTemplate");
        this.insertAviationMessage = new SimpleJdbcInsert(jdbcTemplate.getJdbcTemplate())
                .withTableName("avidb_messages")
                .usingGeneratedKeyColumns("message_id");
        this.insertRejectedAviationMessage = new SimpleJdbcInsert(jdbcTemplate.getJdbcTemplate())
                .withTableName("avidb_rejected_messages");
    }

    @VisibleForTesting
    NamedParameterJdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

    public int insertAviationMessage(final AviationMessage aviationMessage) throws Exception {
        final MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("message_time", aviationMessage.getMessageTime());
        parameters.addValue("station_id", aviationMessage.getStationId()
                .orElseThrow(() -> new IllegalArgumentException("Station id has to be supplied when inserting an aviation message")));
        parameters.addValue("type_id", aviationMessage.getType());
        parameters.addValue("route_id", aviationMessage.getRoute());
        parameters.addValue("message", aviationMessage.getMessage());
        parameters.addValue("valid_from", aviationMessage.getValidFrom().orElse(null));
        parameters.addValue("valid_to", aviationMessage.getValidTo().orElse(null));
        parameters.addValue("created", clock.instant());
        parameters.addValue("file_modified", aviationMessage.getFileModified().orElse(null));
        parameters.addValue("flag", 0);
        parameters.addValue("messir_heading", aviationMessage.getHeading());
        parameters.addValue("version", aviationMessage.getVersion().orElse(null));
        parameters.addValue("format_id", 1);
        try {
            return retryTemplate.execute((RetryCallback<Integer, Exception>) context ->
                    insertAviationMessage.execute(parameters));
        } catch (final Exception e) {
            LOGGER.error("Inserting aviation message {} failed", aviationMessage, e);
            throw e;
        }
    }

    public int insertRejectedAviationMessage(final AviationMessage aviationMessage) throws Exception {
        final MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("icao_code", aviationMessage.getIcaoAirportCode());
        parameters.addValue("message_time", aviationMessage.getMessageTime());
        parameters.addValue("type_id", aviationMessage.getType());
        parameters.addValue("route_id", aviationMessage.getRoute());
        parameters.addValue("message", aviationMessage.getMessage());
        parameters.addValue("valid_from", aviationMessage.getValidFrom().orElse(null));
        parameters.addValue("valid_to", aviationMessage.getValidTo().orElse(null));
        parameters.addValue("created", clock.instant());
        parameters.addValue("file_modified", aviationMessage.getFileModified().orElse(null));
        parameters.addValue("flag", 0);
        parameters.addValue("messir_heading", aviationMessage.getHeading());
        parameters.addValue("reject_reason", aviationMessage.getProcessingResult().getCode());
        parameters.addValue("version", aviationMessage.getVersion().orElse(null));
        try {
            return retryTemplate.execute((RetryCallback<Integer, Exception>) context ->
                    insertRejectedAviationMessage.execute(parameters));
        } catch (final Exception e) {
            LOGGER.error("Inserting rejected aviation message {} failed", aviationMessage, e);
            throw e;
        }
    }

    public Optional<Integer> queryStationId(final String icaoAirportCode) {
        final MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("icao_code", icaoAirportCode);
        try {
            final Integer stationId = retryTemplate.execute((RetryCallback<Integer, Exception>) context ->
                    jdbcTemplate.queryForObject(STATION_ID_QUERY, parameters, Integer.class));
            return Optional.ofNullable(stationId);
        } catch (final EmptyResultDataAccessException ignored) {
            // No station was found
        } catch (final Exception e) {
            LOGGER.error("Querying station id with icao airport code {} failed", icaoAirportCode, e);
        }
        return Optional.empty();
    }

}
