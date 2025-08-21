package fi.fmi.avi.archiver.config;

import fi.fmi.avi.archiver.message.processor.MessageProcessor;
import fi.fmi.avi.archiver.util.StringCaseFormat;
import fi.fmi.avi.archiver.util.instantiation.ConfigValueConverter;
import fi.fmi.avi.archiver.util.instantiation.ObjectFactory;
import fi.fmi.avi.archiver.util.instantiation.PropertyRenamingObjectFactory;
import fi.fmi.avi.archiver.util.instantiation.ReflectionObjectFactory;

import static java.util.Objects.requireNonNull;

public abstract class AbstractMessageProcessorFactoryConfig<P extends MessageProcessor> {
    protected final ConfigValueConverter configValueConverter;

    protected AbstractMessageProcessorFactoryConfig(final ConfigValueConverter configValueConverter) {
        this.configValueConverter = requireNonNull(configValueConverter, "configValueConverter");
    }

    protected <T extends P> ReflectionObjectFactory.Builder<T> builder(final Class<T> type) {
        return ReflectionObjectFactory.builder(type, configValueConverter);
    }

    protected abstract <T extends P> ObjectFactory<T> build(final ReflectionObjectFactory.Builder<T> builder);

    protected <T extends P> ObjectFactory<T> createDecoratedFactory(final ReflectionObjectFactory.Builder<T> builder) {
        return createDecoratedFactory(builder.build());
    }

    protected <T extends P> ObjectFactory<T> createDecoratedFactory(final ObjectFactory<T> factory) {
        return new PropertyRenamingObjectFactory<>(factory, StringCaseFormat::dashToCamel);
    }
}
