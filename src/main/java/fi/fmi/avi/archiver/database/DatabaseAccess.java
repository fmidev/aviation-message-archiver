package fi.fmi.avi.archiver.database;

import com.google.common.annotations.VisibleForTesting;
import fi.fmi.avi.archiver.message.AviationMessage;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;

import java.time.Clock;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public class DatabaseAccess {

    private static final String STATION_ID_QUERY = "select station_id from avidb_stations where icao_code = :icao_code";

    private final Clock clock;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final SimpleJdbcInsert insertAviationMessage;
    private final SimpleJdbcInsert insertRejectedAviationMessage;

    public DatabaseAccess(final NamedParameterJdbcTemplate jdbcTemplate, final Clock clock) {
        this.clock = requireNonNull(clock, "clock");
        this.jdbcTemplate = requireNonNull(jdbcTemplate, "jdbcTemplate");
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

    public int insertAviationMessage(final AviationMessage aviationMessage) {
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
        return insertAviationMessage.execute(parameters);
    }

    public int insertRejectedAviationMessage(final AviationMessage aviationMessage) {
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
        return insertRejectedAviationMessage.execute(parameters);
    }

    public Optional<Integer> queryStationId(final String icaoAirportCode) {
        final MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("icao_code", icaoAirportCode);
        try {
            Integer stationId = jdbcTemplate.queryForObject(STATION_ID_QUERY, parameters, Integer.class);
            return Optional.ofNullable(stationId);
        } catch (final EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

}
