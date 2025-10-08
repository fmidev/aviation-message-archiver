package fi.fmi.avi.archiver.util.instantiation;

import java.util.Map;

public abstract class ForwardingObjectFactoryConfigFactory implements ObjectFactoryConfigFactory {
    protected abstract ObjectFactoryConfigFactory delegate();

    @Override
    public <C extends ObjectFactoryConfig> boolean isValidConfigType(final Class<C> configType) {
        return delegate().isValidConfigType(configType);
    }

    @Override
    public <C extends ObjectFactoryConfig> C create(final Class<C> configType, final Map<?, ?> sourceMap) {
        return delegate().create(configType, sourceMap);
    }

    @Override
    public String toString() {
        return delegate().toString();
    }
}
