package fi.fmi.avi.archiver.message.postaction;

import fi.fmi.avi.archiver.logging.model.ReadableLoggingContext;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Objects.requireNonNull;

/**
 * A dummy test implementation of {@link PostAction} that simply outputs a log message.
 */
public class ResultLogger implements PostAction {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResultLogger.class);

    private final String message;

    public ResultLogger(final String message) {
        this.message = sanitizeMessage(requireNonNull(message, "message"));
    }

    private static String sanitizeMessage(final String message) {
        final String trimmed = message.trim();
        return trimmed.isEmpty() ? "" : trimmed + " ";
    }

    @Override
    public void run(final ReadableLoggingContext context, final ArchiveAviationMessage message) {
        requireNonNull(context, "context");
        requireNonNull(message, "message");
        LOGGER.info("{}Message <{}> {} ({})", this.message, context, message.getArchivalStatus(), message.getProcessingResult());
    }
}
