package fi.fmi.avi.archiver.config.model;

import fi.fmi.avi.archiver.message.processor.populator.MessagePopulator;
import fi.fmi.avi.archiver.util.instantiation.ForwardingObjectFactory;
import fi.fmi.avi.archiver.util.instantiation.ObjectFactory;

import static java.util.Objects.requireNonNull;

/**
 * A typed {@code ObjectFactory} wrapper to ease configuration by type.
 *
 * @param <T> type of message populator
 */
public class MessagePopulatorFactory<T extends MessagePopulator> extends ForwardingObjectFactory<T> {
    private final ObjectFactory<T> messagePopulatorFactory;

    public MessagePopulatorFactory(final ObjectFactory<T> messagePopulatorFactory) {
        this.messagePopulatorFactory = requireNonNull(messagePopulatorFactory, "messagePopulatorFactory");
    }

    @Override
    protected ObjectFactory<T> delegate() {
        return messagePopulatorFactory;
    }
}
