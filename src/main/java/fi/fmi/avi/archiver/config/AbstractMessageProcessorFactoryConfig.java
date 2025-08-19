package fi.fmi.avi.archiver.config;

import fi.fmi.avi.archiver.message.processor.MessageProcessor;
import fi.fmi.avi.archiver.util.StringCaseFormat;
import fi.fmi.avi.archiver.util.instantiation.ConfigValueConverter;
import fi.fmi.avi.archiver.util.instantiation.ObjectFactory;
import fi.fmi.avi.archiver.util.instantiation.PropertyRenamingObjectFactory;
import fi.fmi.avi.archiver.util.instantiation.ReflectionObjectFactory;

import javax.annotation.Nonnull;

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

    protected <T extends P> @Nonnull PropertyRenamingObjectFactory<T> createDelegateFactory(final ReflectionObjectFactory.Builder<T> builder) {
        return new PropertyRenamingObjectFactory<>(builder.build(), StringCaseFormat::dashToCamel);
    }
}
