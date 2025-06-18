package fi.fmi.avi.archiver.message.populator;

import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.MessageProcessorContext;
import fi.fmi.avi.archiver.message.ProcessingResult;

import java.time.Clock;
import java.time.Duration;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

/**
 * Validates that the message time is not too far in the future.
 * <p>
 * The calculated validity period end is exclusive.
 * </p>
 */
public class MessageFutureTimeValidator implements MessagePopulator {

    private final Clock clock;
    private final Duration acceptInFuture;

    public MessageFutureTimeValidator(final Clock clock, final Duration acceptInFuture) {
        this.clock = requireNonNull(clock, "clock");
        requireNonNull(acceptInFuture, "acceptInFuture");
        checkArgument(!acceptInFuture.isNegative() && !acceptInFuture.isZero(), "acceptInFuture must have a positive duration");
        this.acceptInFuture = acceptInFuture;
    }

    @Override
    public void populate(final MessageProcessorContext context, final ArchiveAviationMessage.Builder target) {
        requireNonNull(context, "context");
        requireNonNull(target, "target");
        MessagePopulatorHelper.tryGet(target, ArchiveAviationMessage.Builder::getMessageTime)//
                .ifPresent(messageTime -> {
                    if (Duration.between(clock.instant(), messageTime).compareTo(acceptInFuture) >= 0) {
                        target.setProcessingResult(ProcessingResult.MESSAGE_TIME_IN_FUTURE);
                    }
                });
    }

}
