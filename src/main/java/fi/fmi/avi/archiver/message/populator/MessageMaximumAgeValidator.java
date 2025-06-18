package fi.fmi.avi.archiver.message.populator;

import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.MessageProcessorContext;
import fi.fmi.avi.archiver.message.ProcessingResult;

import java.time.Clock;
import java.time.Duration;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

/**
 * Validates that the message time is not too far in the past.
 * <p>
 * The calculated validity period start is inclusive.
 * </p>
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
    public void populate(final MessageProcessorContext context, final ArchiveAviationMessage.Builder target) {
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
