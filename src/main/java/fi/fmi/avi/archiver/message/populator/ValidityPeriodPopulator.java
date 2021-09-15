package fi.fmi.avi.archiver.message.populator;

import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;

import javax.annotation.Nullable;
import java.time.Duration;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

public class ValidityPeriodPopulator implements MessagePopulator {

    private int typeId;
    private Duration validToOffset;

    public void setTypeId(final int typeId) {
        checkArgument(typeId > 0, "typeId must be positive");
        this.typeId = typeId;
    }

    public void setValidToOffset(final Duration validToOffset) {
        requireNonNull(validToOffset, "validToOffset");
        checkArgument(!validToOffset.isNegative() && !validToOffset.isZero(),
                "validToOffset must have a positive duration");
        this.validToOffset = validToOffset;
    }

    @Override
    public void populate(@Nullable final InputAviationMessage inputAviationMessage, final ArchiveAviationMessage.Builder builder) {
        requireNonNull(builder, "builder");
        if (builder.getType() == typeId) {
            builder.setValidFrom(builder.getMessageTime());
            builder.setValidTo(builder.getMessageTime().plus(validToOffset));
        }
    }

}
