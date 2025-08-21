package fi.fmi.avi.archiver.config.model;

import fi.fmi.avi.archiver.message.processor.populator.MessagePopulator;
import fi.fmi.avi.archiver.util.instantiation.ForwardingObjectFactory;
import fi.fmi.avi.archiver.util.instantiation.ObjectFactory;

import static java.util.Objects.requireNonNull;

public class ForwardingMessagePopulatorFactory<T extends MessagePopulator> extends ForwardingObjectFactory<T> implements MessagePopulatorFactory<T> {
    private final ObjectFactory<T> messagePopulatorFactory;

    public ForwardingMessagePopulatorFactory(final ObjectFactory<T> messagePopulatorFactory) {
        this.messagePopulatorFactory = requireNonNull(messagePopulatorFactory, "messagePopulatorFactory");
    }

    @Override
    protected ObjectFactory<T> delegate() {
        return messagePopulatorFactory;
    }
}
