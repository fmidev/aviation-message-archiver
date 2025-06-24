package fi.fmi.avi.archiver.config;

import fi.fmi.avi.archiver.config.model.MessagePopulatorFactory;
import fi.fmi.avi.archiver.message.processor.populator.MessagePopulator;
import fi.fmi.avi.archiver.util.StringCaseFormat;
import fi.fmi.avi.archiver.util.instantiation.ConfigValueConverter;
import fi.fmi.avi.archiver.util.instantiation.PropertyRenamingObjectFactory;
import fi.fmi.avi.archiver.util.instantiation.ReflectionObjectFactory;

import static java.util.Objects.requireNonNull;

public abstract class AbstractMessagePopulatorFactoryConfig {
    protected final ConfigValueConverter configValueConverter;

    protected AbstractMessagePopulatorFactoryConfig(final ConfigValueConverter configValueConverter) {
        this.configValueConverter = requireNonNull(configValueConverter, "configValueConverter");
    }

    protected <T extends MessagePopulator> ReflectionObjectFactory.Builder<T> builder(final Class<T> type) {
        return ReflectionObjectFactory.builder(type, configValueConverter);
    }

    protected <T extends MessagePopulator> MessagePopulatorFactory<T> build(final ReflectionObjectFactory.Builder<T> builder) {
        return new MessagePopulatorFactory<>(new PropertyRenamingObjectFactory<>(builder.build(), StringCaseFormat::dashToCamel));
    }
}
