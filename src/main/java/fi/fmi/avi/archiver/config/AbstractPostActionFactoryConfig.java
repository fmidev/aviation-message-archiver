package fi.fmi.avi.archiver.config;

import fi.fmi.avi.archiver.config.model.ForwardingPostActionFactory;
import fi.fmi.avi.archiver.config.model.PostActionFactory;
import fi.fmi.avi.archiver.message.processor.postaction.PostAction;
import fi.fmi.avi.archiver.util.instantiation.ConfigValueConverter;
import fi.fmi.avi.archiver.util.instantiation.ReflectionObjectFactory;

public abstract class AbstractPostActionFactoryConfig extends AbstractMessageProcessorFactoryConfig<PostAction> {
    protected AbstractPostActionFactoryConfig(final ConfigValueConverter configValueConverter) {
        super(configValueConverter);
    }

    protected <T extends PostAction> PostActionFactory<T> build(final ReflectionObjectFactory.Builder<T> builder) {
        return new ForwardingPostActionFactory<>(createDelegateFactory(builder));
    }
}
