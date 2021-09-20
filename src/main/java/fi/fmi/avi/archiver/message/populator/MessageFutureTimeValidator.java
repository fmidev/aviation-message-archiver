package fi.fmi.avi.archiver.message.populator;

import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.ProcessingResult;

import javax.annotation.Nullable;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

/**
 * Validates that the message time is not too far in the future.
 * <p>
 * The calculated validity period end is exclusive.
 */
public class MessageFutureTimeValidator implements MessagePopulator {

    private final Clock clock;
    private final Duration maximumFutureTime;

    public MessageFutureTimeValidator(final Clock clock, final Duration maximumFutureTime) {
        this.clock = requireNonNull(clock, "clock");
        requireNonNull(maximumFutureTime, "maximumFutureTime");
        checkArgument(!maximumFutureTime.isNegative() && !maximumFutureTime.isZero(),
                "maximumFutureTime must have a positive duration");
        this.maximumFutureTime = maximumFutureTime;
    }

    @Override
    public void populate(@Nullable final InputAviationMessage inputAviationMessage, final ArchiveAviationMessage.Builder builder) {
        requireNonNull(builder, "builder");
        final Instant now = clock.instant();
        final Instant future = now.plus(maximumFutureTime);
        if (builder.getMessageTime().equals(future) || builder.getMessageTime().isAfter(future)) {
            builder.setProcessingResult(ProcessingResult.MESSAGE_TIME_IN_FUTURE);
        }
    }

}
