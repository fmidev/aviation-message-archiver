package fi.fmi.avi.archiver.message.populator;

import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;

import javax.annotation.Nullable;
import java.time.Duration;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

public class ValidityPeriodPopulator implements MessagePopulator {

    private final int messageTypeId;
    private final Duration validityEndOffset;

    public ValidityPeriodPopulator(final int messageTypeId, final Duration validityEndOffset) {
        checkArgument(messageTypeId > 0, "typeId must be positive");
        this.messageTypeId = messageTypeId;

        requireNonNull(validityEndOffset, "validityEndOffset");
        checkArgument(!validityEndOffset.isNegative() && !validityEndOffset.isZero(),
                "validityEndOffset must have a positive duration");
        this.validityEndOffset = validityEndOffset;
    }

    @Override
    public void populate(@Nullable final InputAviationMessage inputAviationMessage, final ArchiveAviationMessage.Builder builder) {
        requireNonNull(builder, "builder");
        if (builder.getType() == messageTypeId) {
            builder.setValidFrom(builder.getMessageTime());
            builder.setValidTo(builder.getMessageTime().plus(validityEndOffset));
        }
    }

}
