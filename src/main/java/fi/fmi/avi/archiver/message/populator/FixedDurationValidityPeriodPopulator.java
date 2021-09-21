package fi.fmi.avi.archiver.message.populator;

import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.model.MessageType;

import javax.annotation.Nullable;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

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
        final OptionalInt messageType = MessagePopulatorHelper.tryGetInt(builder, ArchiveAviationMessage.Builder::getType);
        final Optional<Instant> messageTime = MessagePopulatorHelper.tryGet(builder, ArchiveAviationMessage.Builder::getMessageTime);
        if (messageType.isPresent() && messageTime.isPresent()) {
            if (messageType.getAsInt() == messageTypeId) {
                final Instant time = messageTime.get();
                builder.setValidFrom(time);
                builder.setValidTo(time.plus(validityEndOffset));
            }
        }
    }

}
