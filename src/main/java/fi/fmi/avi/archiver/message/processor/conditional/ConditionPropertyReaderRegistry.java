package fi.fmi.avi.archiver.message.processor.conditional;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

public class ConditionPropertyReaderRegistry implements ConditionPropertyReaderFactory {
    private final Map<String, ConditionPropertyReader<?>> propertyReaders = new HashMap<>();

    @Override
    public ConditionPropertyReader<?> getInstance(final String propertyName) {
        requireNonNull(propertyName, "propertyName");
        @Nullable final ConditionPropertyReader<?> conditionPropertyReader = propertyReaders.get(propertyName);
        if (conditionPropertyReader == null) {
            throw new IllegalArgumentException("Unknown property: " + propertyName);
        }
        return conditionPropertyReader;
    }

    public void register(final ConditionPropertyReader<?> conditionPropertyReader) {
        requireNonNull(conditionPropertyReader, "conditionPropertyReader");
        register(conditionPropertyReader.getPropertyName(), conditionPropertyReader);
    }

    public void register(final String name, final ConditionPropertyReader<?> conditionPropertyReader) {
        requireNonNull(name, "name");
        requireNonNull(conditionPropertyReader, "conditionPropertyReader");
        if (propertyReaders.containsKey(name)) {
            throw new IllegalArgumentException("A ConditionPropertyReader with same name already exists: " + name);
        }
        propertyReaders.put(name, conditionPropertyReader);
    }

    public void unregister(final String name) {
        requireNonNull(name, "name");
        propertyReaders.remove(name);
    }

}
