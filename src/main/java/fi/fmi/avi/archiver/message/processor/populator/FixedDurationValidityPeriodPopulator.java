package fi.fmi.avi.archiver.message.processor.populator;

import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.processor.MessageProcessorContext;
import fi.fmi.avi.archiver.message.processor.MessageProcessorHelper;

import java.time.Duration;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

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
    public void populate(final MessageProcessorContext context, final ArchiveAviationMessage.Builder target) {
        requireNonNull(context, "context");
        requireNonNull(target, "target");
        MessageProcessorHelper.tryGet(target, ArchiveAviationMessage.Builder::getMessageTime)//
                .ifPresent(messageTime -> {
                    target.setValidFrom(messageTime);
                    target.setValidTo(messageTime.plus(validityEndOffset));
                });
    }
}
