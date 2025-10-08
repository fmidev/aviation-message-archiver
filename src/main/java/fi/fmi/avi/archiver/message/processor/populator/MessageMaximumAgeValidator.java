package fi.fmi.avi.archiver.message.processor.populator;

import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.ProcessingResult;
import fi.fmi.avi.archiver.message.processor.MessageProcessorContext;
import fi.fmi.avi.archiver.message.processor.MessageProcessorHelper;
import fi.fmi.avi.archiver.message.processor.conditional.MessageAgePropertyReader;

import java.time.Clock;
import java.time.Duration;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

/**
 * Validates that the message time is not too far in the past.
 * <p>
 * The calculated validity period start is inclusive.
 * </p>
 *
 * @deprecated in favor of {@link FixedProcessingResultPopulator} and conditional populator activation using {@link MessageAgePropertyReader}
 */
@Deprecated(forRemoval = true)
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
        MessageProcessorHelper.tryGet(target, ArchiveAviationMessage.Builder::getMessageTime)//
                .ifPresent(messageTime -> {
                    if (Duration.between(messageTime, clock.instant()).compareTo(maximumAge) > 0) {
                        target.setProcessingResult(ProcessingResult.MESSAGE_TOO_OLD);
                    }
                });
    }

}
