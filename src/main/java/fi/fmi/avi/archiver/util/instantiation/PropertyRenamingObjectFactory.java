package fi.fmi.avi.archiver.util.instantiation;

import java.util.Map;
import java.util.function.UnaryOperator;

import static java.util.Objects.requireNonNull;

public class PropertyRenamingObjectFactory<T> extends ForwardingObjectFactory<T> {
    private final ObjectFactory<T> delegate;
    private final UnaryOperator<String> renameOperator;

    public PropertyRenamingObjectFactory(final ObjectFactory<T> delegate, final UnaryOperator<String> renameOperator) {
        this.delegate = requireNonNull(delegate, "delegate");
        this.renameOperator = requireNonNull(renameOperator, "renameOperator");
    }

    @Override
    protected ObjectFactory<T> delegate() {
        return delegate;
    }

    @Override
    public T newInstance(final Map<String, Object> config) {
        return super.newInstance(ObjectFactoryConfigUtils.renameProperties(config, renameOperator));
    }
}
