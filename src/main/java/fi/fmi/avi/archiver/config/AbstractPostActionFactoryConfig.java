package fi.fmi.avi.archiver.config;

import fi.fmi.avi.archiver.config.model.PostActionFactory;
import fi.fmi.avi.archiver.message.processor.postaction.PostAction;
import fi.fmi.avi.archiver.util.StringCaseFormat;
import fi.fmi.avi.archiver.util.instantiation.ConfigValueConverter;
import fi.fmi.avi.archiver.util.instantiation.PropertyRenamingObjectFactory;
import fi.fmi.avi.archiver.util.instantiation.ReflectionObjectFactory;

import static java.util.Objects.requireNonNull;

public abstract class AbstractPostActionFactoryConfig {
    protected final ConfigValueConverter configValueConverter;

    protected AbstractPostActionFactoryConfig(final ConfigValueConverter configValueConverter) {
        this.configValueConverter = requireNonNull(configValueConverter, "configValueConverter");
    }

    protected <T extends PostAction> ReflectionObjectFactory.Builder<T> builder(final Class<T> type) {
        return ReflectionObjectFactory.builder(type, configValueConverter);
    }

    protected <T extends PostAction> PostActionFactory<T> build(final ReflectionObjectFactory.Builder<T> builder) {
        return new PostActionFactory<>(new PropertyRenamingObjectFactory<>(builder.build(), StringCaseFormat::dashToCamel));
    }
}
