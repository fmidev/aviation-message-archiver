package fi.fmi.avi.archiver.message.populator;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import java.util.Map;

import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.model.MessageType;

public class FixedTypePopulator implements MessagePopulator {

    private final int type;

    public FixedTypePopulator(final Map<MessageType, Integer> typeIds, final MessageType type) {
        requireNonNull(typeIds, "typeIds");
        requireNonNull(type, "type");
        checkArgument(typeIds.containsKey(type), "Unknown type: %s", type);
        this.type = typeIds.get(type);
    }

    @Override
    public void populate(final MessagePopulatingContext context, final ArchiveAviationMessage.Builder target) {
        requireNonNull(context, "context");
        requireNonNull(target, "target");
        target.setType(type);
    }
}
