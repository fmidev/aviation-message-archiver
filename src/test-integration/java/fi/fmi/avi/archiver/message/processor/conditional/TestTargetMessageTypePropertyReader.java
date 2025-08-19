package fi.fmi.avi.archiver.message.processor.conditional;

import com.google.common.collect.BiMap;
import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessageOrBuilder;
import fi.fmi.avi.archiver.message.processor.MessageProcessorHelper;
import fi.fmi.avi.model.MessageType;

import javax.annotation.Nullable;
import java.util.OptionalInt;

import static java.util.Objects.requireNonNull;

public class TestTargetMessageTypePropertyReader extends AbstractConditionPropertyReader<MessageType> {
    private final BiMap<MessageType, Integer> messageTypeIds;

    public TestTargetMessageTypePropertyReader(final BiMap<MessageType, Integer> messageTypeIds) {
        this.messageTypeIds = requireNonNull(messageTypeIds, "messageTypeIds");
    }

    @Nullable
    @Override
    public MessageType readValue(final InputAviationMessage input, final ArchiveAviationMessageOrBuilder target) {
        requireNonNull(input, "input");
        requireNonNull(target, "target");
        final OptionalInt messageTypeId = MessageProcessorHelper.tryGetInt(target, ArchiveAviationMessageOrBuilder::getType);
        return messageTypeId.isPresent() ? messageTypeIds.inverse().get(messageTypeId.getAsInt()) : null;
    }

    @Override
    public boolean validate(final MessageType value) {
        requireNonNull(value, "value");
        return messageTypeIds.containsKey(value);
    }
}
