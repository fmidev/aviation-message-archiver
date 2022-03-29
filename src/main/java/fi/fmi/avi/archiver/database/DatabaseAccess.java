package fi.fmi.avi.archiver.database;

import static java.util.Objects.requireNonNull;

import java.sql.Types;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.retry.RetryContext;
import org.springframework.retry.support.RetryTemplate;

import com.google.common.annotations.VisibleForTesting;

import fi.fmi.avi.archiver.logging.model.ReadableLoggingContext;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessageIWXXMDetails;
import fi.fmi.avi.archiver.spring.retry.ArchiverRetryContexts;

/**
 * Database access operations.
 */
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

        this.insertAviationMessage = new SimpleJdbcInsert(jdbcTemplate.getJdbcTemplate())//
                .withSchemaName(schema)//
                .withTableName("avidb_messages")//
                .usingGeneratedKeyColumns("message_id");
        this.insertRejectedAviationMessage = new SimpleJdbcInsert(jdbcTemplate.getJdbcTemplate())//
                .withSchemaName(schema)//
                .withTableName("avidb_rejected_messages")//
                .usingGeneratedKeyColumns("rejected_message_id");
        this.insertIwxxmDetails = new SimpleJdbcInsert(jdbcTemplate.getJdbcTemplate())//
                .withSchemaName(schema)//
                .withTableName("avidb_message_iwxxm_details")//
                .usingGeneratedKeyColumns("id");
        this.insertRejectedIwxxmDetails = new SimpleJdbcInsert(jdbcTemplate.getJdbcTemplate())//
                .withSchemaName(schema)//
                .withTableName("avidb_rejected_message_iwxxm_details");
        this.stationIdQuery = "select station_id from " + schema + ".avidb_stations where icao_code = :icao_code";
    }

    private static void addTimestampWithTimezone(final MapSqlParameterSource parameters, final String name, @Nullable final Instant timestamp) {
        parameters.addValue(name, timestamp == null ? null : timestamp.atOffset(ZoneOffset.UTC), Types.TIMESTAMP_WITH_TIMEZONE);
    }

    @VisibleForTesting
    NamedParameterJdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

    /**
     * Insert aviation message into the main message table. Returns the generated id.
     *
     * @param archiveAviationMessage
     *         aviation message to archive
     * @param loggingContext
     *         logging context
     *
     * @return the generated id
     */
    public Number insertAviationMessage(final ArchiveAviationMessage archiveAviationMessage, final ReadableLoggingContext loggingContext) {
        requireNonNull(archiveAviationMessage, "archiveAviationMessage");
        requireNonNull(loggingContext, "loggingContext");
        final MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("station_id", archiveAviationMessage.getStationId()//
                .orElseThrow(() -> new IllegalArgumentException(String.format("Message <%s> is missing stationId.", loggingContext))));
        addCommonParameters(parameters, archiveAviationMessage);
        final Number id = retryTemplate.execute(context -> {
            initRetryContext(context, "insert message", loggingContext);
            return insertAviationMessage.executeAndReturnKey(parameters);
        });
        LOGGER.debug("Inserted message <{}> id:{}.", loggingContext, id);
        if (archiveAviationMessage.getIWXXMDetails().isEmpty()) {
            LOGGER.debug("Message id:{} contains no IWXXM details.", id);
        } else {
            insertIwxxmDetails(id, archiveAviationMessage.getIWXXMDetails(), loggingContext);
        }
        return id;
    }

    /**
     * Insert aviation message into the rejected messages table. Returns the generated id.
     *
     * @param archiveAviationMessage
     *         aviation message to archive in the rejected messages table
     * @param loggingContext
     *         logging context
     *
     * @return the generated id
     */
    public Number insertRejectedAviationMessage(final ArchiveAviationMessage archiveAviationMessage, final ReadableLoggingContext loggingContext) {
        requireNonNull(archiveAviationMessage, "archiveAviationMessage");
        requireNonNull(loggingContext, "loggingContext");
        final MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("icao_code", archiveAviationMessage.getStationIcaoCode()).addValue("reject_reason", archiveAviationMessage.getProcessingResult().getCode());
        addCommonParameters(parameters, archiveAviationMessage);
        final Number id = retryTemplate.execute(context -> {
            initRetryContext(context, "insert rejected message", loggingContext);
            return insertRejectedAviationMessage.executeAndReturnKey(parameters);
        });
        LOGGER.debug("Inserted rejected message <{}> id:{}; reject reason: {}({})", loggingContext, id, archiveAviationMessage.getProcessingResult().getCode(),
                archiveAviationMessage.getProcessingResult());
        if (archiveAviationMessage.getIWXXMDetails().isEmpty()) {
            LOGGER.debug("Rejected message id:{} contains no IWXXM details.", id);
        } else {
            insertRejectedIwxxmDetails(id, archiveAviationMessage.getIWXXMDetails(), loggingContext);
        }
        return id;
    }

    private void insertIwxxmDetails(final Number messageId, final ArchiveAviationMessageIWXXMDetails iwxxmDetails, final ReadableLoggingContext loggingContext) {
        final MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("message_id", messageId).addValue("collect_identifier", iwxxmDetails.getCollectIdentifier().orElse(null)).addValue("iwxxm_version", iwxxmDetails.getXMLNamespace().orElse(null));
        retryTemplate.execute(context -> {
            initRetryContext(context, "insert IWXXM details id:" + messageId, loggingContext);
            return insertIwxxmDetails.execute(parameters);
        });
        LOGGER.debug("Inserted IWXXM details of message <{}> id:{}.", loggingContext, messageId);
    }

    private void insertRejectedIwxxmDetails(final Number rejectedMessageId, final ArchiveAviationMessageIWXXMDetails iwxxmDetails, final ReadableLoggingContext loggingContext) {
        final MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("rejected_message_id", rejectedMessageId)
                .addValue("collect_identifier", iwxxmDetails.getCollectIdentifier().orElse(null))
                .addValue("iwxxm_version", iwxxmDetails.getXMLNamespace().orElse(null));
        retryTemplate.execute(context -> {
            initRetryContext(context, "insert rejected IWXXM details id:" + rejectedMessageId, loggingContext);
            return insertRejectedIwxxmDetails.execute(parameters);
        });
        LOGGER.debug("Inserted IWXXM details of rejected message <{}> id:{}.", loggingContext, rejectedMessageId);
    }

    /**
     * Return the station id matching provided {@code stationIcaoCode}, if exists.
     *
     * @param stationIcaoCode
     *         ICAO code to look for
     * @param loggingContext
     *         logging context
     *
     * @return station id or empty if database does not contain provided {@code stationIcaoCode} or in case of an error
     */
    public Optional<Integer> queryStationId(final String stationIcaoCode, final ReadableLoggingContext loggingContext) {
        requireNonNull(stationIcaoCode, "stationIcaoCode");
        requireNonNull(loggingContext, "loggingContext");
        final MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("icao_code", stationIcaoCode);
        try {
            final Integer stationId = retryTemplate.execute(context -> {
                initRetryContext(context, "query station id: " + stationIcaoCode, loggingContext);
                return jdbcTemplate.queryForObject(stationIdQuery, parameters, Integer.class);
            });
            return Optional.ofNullable(stationId);
        } catch (final RuntimeException e) {
            return Optional.empty();
        }
    }

    private void initRetryContext(final RetryContext context, final String databaseOperation, final ReadableLoggingContext loggingContext) {
        ArchiverRetryContexts.DATABASE_OPERATION.set(context, databaseOperation);
        ArchiverRetryContexts.LOGGING_CONTEXT.set(context, loggingContext);
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
}
