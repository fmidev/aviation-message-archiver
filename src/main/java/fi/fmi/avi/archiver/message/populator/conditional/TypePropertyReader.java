package fi.fmi.avi.archiver.message.populator.conditional;

import static java.util.Objects.requireNonNull;

import javax.annotation.Nullable;

import com.google.common.collect.BiMap;

import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.populator.MessagePopulatorHelper;
import fi.fmi.avi.model.MessageType;

public class TypePropertyReader extends AbstractConditionPropertyReader<MessageType> {
    private final BiMap<MessageType, Integer> messageTypeIds;

    public TypePropertyReader(final BiMap<MessageType, Integer> messageTypeIds) {
        this.messageTypeIds = requireNonNull(messageTypeIds, "messageTypeIds");
    }

    @Nullable
    @Override
    public MessageType readValue(final InputAviationMessage input, final ArchiveAviationMessage.Builder target) {
        requireNonNull(input, "input");
        requireNonNull(target, "target");
        final int typeId = MessagePopulatorHelper.tryGetInt(target, builder -> builder.getType()).orElse(Integer.MIN_VALUE);
        return messageTypeIds.inverse().get(typeId);
    }

    @Override
    public boolean validate(final MessageType value) {
        requireNonNull(value, "value");
        return messageTypeIds.containsKey(value);
    }
}
