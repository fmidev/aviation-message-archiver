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
        if (builder.getMessageTime().isAfter(now.plus(maximumFutureTime))) {
            builder.setProcessingResult(ProcessingResult.MESSAGE_TIME_IN_FUTURE);
        }
    }

}
