package fi.fmi.avi.archiver.message.populator;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import java.time.Duration;

import fi.fmi.avi.archiver.message.ArchiveAviationMessage;

/**
 * Populate validity period in {@link ArchiveAviationMessage.Builder} with a fixed duration period starting from
 * {@link ArchiveAviationMessage#getMessageTime() message time}.
 * This populator uses {@link ArchiveAviationMessage.Builder} as input, and therefore must be executed after message time is populated. In case message time
 * is missing, this populator does nothing.
 */
public class FixedDurationValidityPeriodPopulator implements MessagePopulator {
    private final Duration validityEndOffset;

    public FixedDurationValidityPeriodPopulator(final Duration validityEndOffset) {
        this.validityEndOffset = requireNonNull(validityEndOffset, "validityEndOffset");
        checkArgument(!validityEndOffset.isNegative() && !validityEndOffset.isZero(), "validityEndOffset must have a positive duration");
    }

    @Override
    public void populate(final MessagePopulatingContext context, final ArchiveAviationMessage.Builder target) {
        requireNonNull(context, "context");
        requireNonNull(target, "target");
        MessagePopulatorHelper.tryGet(target, ArchiveAviationMessage.Builder::getMessageTime)//
                .ifPresent(messageTime -> {
                    target.setValidFrom(messageTime);
                    target.setValidTo(messageTime.plus(validityEndOffset));
                });
    }
}
