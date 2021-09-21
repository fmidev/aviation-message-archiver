package fi.fmi.avi.archiver.message.populator;

import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.model.MessageType;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

public class FixedDurationValidityPeriodPopulator implements MessagePopulator {

    private final int messageTypeId;
    private final Duration validityEndOffset;

    public FixedDurationValidityPeriodPopulator(final Map<MessageType, Integer> typeIds, final MessageType messageType,
                                                final Duration validityEndOffset) {
        requireNonNull(typeIds, "typeIds");
        requireNonNull(messageType, "messageType");
        checkArgument(typeIds.containsKey(messageType), "messageType must have a configured type id");
        this.messageTypeId = typeIds.get(messageType);

        requireNonNull(validityEndOffset, "validityEndOffset");
        checkArgument(!validityEndOffset.isNegative() && !validityEndOffset.isZero(),
                "validityEndOffset must have a positive duration");
        this.validityEndOffset = validityEndOffset;
    }

    @Override
    public void populate(@Nullable final InputAviationMessage inputAviationMessage, final ArchiveAviationMessage.Builder builder) {
        requireNonNull(builder, "builder");
        MessagePopulatorHelper.tryGet(builder, ArchiveAviationMessage.Builder::getMessageTime)//
                .ifPresent(messageTime -> {
                    if (builder.getType() == messageTypeId) {
                        builder.setValidFrom(messageTime);
                        builder.setValidTo(messageTime.plus(validityEndOffset));
                    }
                });
    }

}
