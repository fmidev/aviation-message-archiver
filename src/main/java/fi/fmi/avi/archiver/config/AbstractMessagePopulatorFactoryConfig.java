package fi.fmi.avi.archiver.config;

import fi.fmi.avi.archiver.config.model.ForwardingMessagePopulatorFactory;
import fi.fmi.avi.archiver.config.model.MessagePopulatorFactory;
import fi.fmi.avi.archiver.message.processor.populator.MessagePopulator;
import fi.fmi.avi.archiver.util.instantiation.ConfigValueConverter;
import fi.fmi.avi.archiver.util.instantiation.ReflectionObjectFactory;

public abstract class AbstractMessagePopulatorFactoryConfig extends AbstractMessageProcessorFactoryConfig<MessagePopulator> {
    protected AbstractMessagePopulatorFactoryConfig(final ConfigValueConverter configValueConverter) {
        super(configValueConverter);
    }

    @Override
    protected <T extends MessagePopulator> MessagePopulatorFactory<T> build(final ReflectionObjectFactory.Builder<T> builder) {
        return new ForwardingMessagePopulatorFactory<>(createDelegateFactory(builder));
    }
}
