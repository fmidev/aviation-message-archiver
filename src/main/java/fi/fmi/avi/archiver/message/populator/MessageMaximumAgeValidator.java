package fi.fmi.avi.archiver.message.populator;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import java.time.Clock;
import java.time.Duration;

import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.ProcessingResult;

/**
 * Validates that the message time is not too far in the past.
 * <p>
 * The calculated validity period start is inclusive.
 */
public class MessageMaximumAgeValidator implements MessagePopulator {

    private final Clock clock;
    private final Duration maximumAge;

    public MessageMaximumAgeValidator(final Clock clock, final Duration maximumAge) {
        this.clock = requireNonNull(clock, "clock");
        requireNonNull(maximumAge, "maximumAge");
        checkArgument(!maximumAge.isNegative() && !maximumAge.isZero(), "maximumAge must have a positive duration");
        this.maximumAge = maximumAge;
    }

    @Override
    public void populate(final MessagePopulatingContext context, final ArchiveAviationMessage.Builder target) {
        requireNonNull(context, "context");
        requireNonNull(target, "target");
        MessagePopulatorHelper.tryGet(target, ArchiveAviationMessage.Builder::getMessageTime)//
                .ifPresent(messageTime -> {
                    if (Duration.between(messageTime, clock.instant()).compareTo(maximumAge) > 0) {
                        target.setProcessingResult(ProcessingResult.MESSAGE_TOO_OLD);
                    }
                });
    }

}
