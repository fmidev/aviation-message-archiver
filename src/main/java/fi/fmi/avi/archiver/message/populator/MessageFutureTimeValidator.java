package fi.fmi.avi.archiver.message.populator;

import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.ProcessingResult;

import javax.annotation.Nullable;
import java.time.Clock;
import java.time.Duration;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

/**
 * Validates that the message time is not too far in the future.
 * <p>
 * The calculated validity period end is exclusive.
 */
public class MessageFutureTimeValidator implements MessagePopulator {

    private final Clock clock;
    private final Duration acceptInFuture;

    public MessageFutureTimeValidator(final Clock clock, final Duration acceptInFuture) {
        this.clock = requireNonNull(clock, "clock");
        requireNonNull(acceptInFuture, "acceptInFuture");
        checkArgument(!acceptInFuture.isNegative() && !acceptInFuture.isZero(),
                "acceptInFuture must have a positive duration");
        this.acceptInFuture = acceptInFuture;
    }

    @Override
    public void populate(@Nullable final InputAviationMessage inputAviationMessage, final ArchiveAviationMessage.Builder builder) {
        requireNonNull(builder, "builder");
        MessagePopulatorHelper.tryGet(builder, ArchiveAviationMessage.Builder::getMessageTime)//
                .ifPresent(messageTime -> {
                    if (Duration.between(clock.instant(), messageTime).compareTo(acceptInFuture) >= 0) {
                        builder.setProcessingResult(ProcessingResult.MESSAGE_TIME_IN_FUTURE);
                    }
                });
    }

}
