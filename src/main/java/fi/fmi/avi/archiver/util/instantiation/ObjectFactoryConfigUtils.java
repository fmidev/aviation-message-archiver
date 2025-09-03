package fi.fmi.avi.archiver.util.instantiation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.UnaryOperator;

import static java.util.Objects.requireNonNull;

final class ObjectFactoryConfigUtils {
    private ObjectFactoryConfigUtils() {
        throw new AssertionError();
    }

    public static Map<String, Object> renameProperties(final Map<?, ?> config, final UnaryOperator<String> renameOperator) {
        requireNonNull(config, "config");
        requireNonNull(renameOperator, "renameOperator");

        final HashMap<String, Object> transformedConfig = new HashMap<>();
        for (final Map.Entry<?, ?> entry : config.entrySet()) {
            final String transformedPropertyName = renameOperator.apply(entry.getKey().toString());
            if (transformedConfig.containsKey(transformedPropertyName)) {
                throw new IllegalArgumentException(
                        String.format(Locale.ROOT, "Duplicate config property name: '%s' ('%s')", transformedPropertyName, entry.getKey()));
            }
            transformedConfig.put(transformedPropertyName, entry.getValue());
        }
        return Collections.unmodifiableMap(transformedConfig);
    }
}
