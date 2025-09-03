package fi.fmi.avi.archiver.util.instantiation;

import java.util.Map;
import java.util.function.UnaryOperator;

import static java.util.Objects.requireNonNull;

public class PropertyRenamingObjectFactoryConfigFactory extends ForwardingObjectFactoryConfigFactory {
    private final ObjectFactoryConfigFactory delegate;
    private final UnaryOperator<String> renameOperator;

    public PropertyRenamingObjectFactoryConfigFactory(final ObjectFactoryConfigFactory delegate, final UnaryOperator<String> renameOperator) {
        this.delegate = requireNonNull(delegate, "delegate");
        this.renameOperator = requireNonNull(renameOperator, "renameOperator");
    }

    @Override
    protected ObjectFactoryConfigFactory delegate() {
        return delegate;
    }

    @Override
    public <C extends ObjectFactoryConfig> C create(final Class<C> configType, final Map<?, ?> sourceMap) {
        return super.create(configType, ObjectFactoryConfigUtils.renameProperties(sourceMap, renameOperator));
    }
}
