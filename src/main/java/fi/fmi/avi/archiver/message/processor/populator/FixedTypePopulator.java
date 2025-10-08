package fi.fmi.avi.archiver.message.processor.populator;

import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.processor.MessageProcessorContext;
import fi.fmi.avi.model.MessageType;

import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

/**
 * Set a fixed {@link ArchiveAviationMessage#getType() type} on message.
 * This populator is typically composed as {@link ConditionalMessagePopulator} to limit affected messages.
 */
public class FixedTypePopulator implements MessagePopulator {

    private final int type;

    public FixedTypePopulator(final Map<MessageType, Integer> typeIds, final MessageType type) {
        requireNonNull(typeIds, "typeIds");
        requireNonNull(type, "type");
        checkArgument(typeIds.containsKey(type), "Unknown type: %s", type);
        this.type = typeIds.get(type);
    }

    @Override
    public void populate(final MessageProcessorContext context, final ArchiveAviationMessage.Builder target) {
        requireNonNull(context, "context");
        requireNonNull(target, "target");
        target.setType(type);
    }
}
