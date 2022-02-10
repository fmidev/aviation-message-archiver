package fi.fmi.avi.archiver.util.instantiation;

import java.util.Map;

public abstract class ForwardingObjectFactory<T> implements ObjectFactory<T> {
    protected abstract ObjectFactory<T> delegate();

    @Override
    public Class<T> getType() {
        return delegate().getType();
    }

    @Override
    public String getName() {
        return delegate().getName();
    }

    @Override
    public T newInstance(final Map<String, Object> config) {
        return delegate().newInstance(config);
    }

    @Override
    public String toString() {
        return delegate().toString();
    }
}
