package fi.fmi.avi.archiver.message.populator;

import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.ProcessingResult;

import javax.annotation.Nullable;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import static java.util.Objects.requireNonNull;

public class MessageTimeValidator implements MessagePopulator {

    private final Clock clock;
    private Duration maximumAge;
    private Duration maximumFutureTime;

    public MessageTimeValidator(final Clock clock) {
        this.clock = requireNonNull(clock, "clock");
    }

    public void setMaximumAge(final Duration maximumAge) {
        this.maximumAge = requireNonNull(maximumAge, "maximumAge");
    }

    public void setMaximumFutureTime(final Duration maximumFutureTime) {
        this.maximumFutureTime = requireNonNull(maximumFutureTime, "maximumFutureTime");
    }

    @Override
    public void populate(@Nullable final InputAviationMessage inputAviationMessage, final ArchiveAviationMessage.Builder builder) {
        requireNonNull(builder, "builder");
        final Instant now = clock.instant();
        if (builder.getMessageTime().isBefore(now.minus(maximumAge))) {
            builder.setProcessingResult(ProcessingResult.MESSAGE_TOO_OLD);
        } else if (builder.getMessageTime().isAfter(now.plus(maximumFutureTime))) {
            builder.setProcessingResult(ProcessingResult.MESSAGE_TIME_IN_FUTURE);
        }
    }

}
