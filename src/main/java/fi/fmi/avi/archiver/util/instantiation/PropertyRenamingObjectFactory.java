package fi.fmi.avi.archiver.util.instantiation;

import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.UnaryOperator;

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
        return super.newInstance(transformConfig(config));
    }

    private HashMap<String, Object> transformConfig(final Map<String, Object> config) {
        final HashMap<String, Object> transformedConfig = new HashMap<>();
        for (final Map.Entry<String, Object> entry : config.entrySet()) {
            final String transformedPropertyName = renameOperator.apply(entry.getKey());
            if (transformedConfig.containsKey(transformedPropertyName)) {
                throw new IllegalArgumentException(
                        String.format(Locale.ROOT, "Duplicate config property name: '%s' ('%s')", transformedPropertyName, entry.getKey()));
            }
            transformedConfig.put(transformedPropertyName, entry.getValue());
        }
        return transformedConfig;
    }

}
