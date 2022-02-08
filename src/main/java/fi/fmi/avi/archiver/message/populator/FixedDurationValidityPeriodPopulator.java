package fi.fmi.avi.archiver.message.populator;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import java.time.Duration;

import javax.annotation.Nullable;

import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;

public class FixedDurationValidityPeriodPopulator implements MessagePopulator {
    private final Duration validityEndOffset;

    public FixedDurationValidityPeriodPopulator(final Duration validityEndOffset) {
        this.validityEndOffset = requireNonNull(validityEndOffset, "validityEndOffset");
        checkArgument(!validityEndOffset.isNegative() && !validityEndOffset.isZero(), "validityEndOffset must have a positive duration");
    }

    @Override
    public void populate(@Nullable final InputAviationMessage input, final ArchiveAviationMessage.Builder target) {
        requireNonNull(target, "target");
        MessagePopulatorHelper.tryGet(target, ArchiveAviationMessage.Builder::getMessageTime)//
                .ifPresent(messageTime -> {
                    target.setValidFrom(messageTime);
                    target.setValidTo(messageTime.plus(validityEndOffset));
                });
    }
}
