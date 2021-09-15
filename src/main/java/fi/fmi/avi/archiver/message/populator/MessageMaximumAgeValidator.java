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

public class MessageMaximumAgeValidator implements MessagePopulator {

    private final Clock clock;
    private Duration maximumAge;

    public MessageMaximumAgeValidator(final Clock clock) {
        this.clock = requireNonNull(clock, "clock");
    }

    public void setMaximumAge(final Duration maximumAge) {
        requireNonNull(maximumAge, "maximumAge");
        checkArgument(!maximumAge.isNegative() && !maximumAge.isZero(),
                "maximumAge must have a positive duration");
        this.maximumAge = maximumAge;
    }

    @Override
    public void populate(@Nullable final InputAviationMessage inputAviationMessage, final ArchiveAviationMessage.Builder builder) {
        requireNonNull(builder, "builder");
        final Instant now = clock.instant();
        if (builder.getMessageTime().isBefore(now.minus(maximumAge))) {
            builder.setProcessingResult(ProcessingResult.MESSAGE_TOO_OLD);
        }
    }

}
