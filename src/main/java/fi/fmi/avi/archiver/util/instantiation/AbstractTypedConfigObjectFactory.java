package fi.fmi.avi.archiver.util.instantiation;

import java.util.Map;

import static java.util.Objects.requireNonNull;

public abstract class AbstractTypedConfigObjectFactory<T, C extends ObjectFactoryConfig> implements ObjectFactory<T> {

    private final ObjectFactoryConfigFactory configFactory;

    protected AbstractTypedConfigObjectFactory(final ObjectFactoryConfigFactory configFactory) {
        this.configFactory = requireNonNull(configFactory, "configFactory");
    }

    public abstract Class<C> getConfigType();

    @Override
    public final T newInstance(final Map<String, Object> config) {
        requireNonNull(config, "config");
        return newInstance(configFactory.create(getConfigType(), config));
    }

    public abstract T newInstance(final C config);

}
