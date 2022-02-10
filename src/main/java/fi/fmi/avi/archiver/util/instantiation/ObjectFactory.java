package fi.fmi.avi.archiver.util.instantiation;

import java.util.Map;

public interface ObjectFactory<T> {
    Class<T> getType();

    default String getName() {
        return getType().getSimpleName();
    }

    T newInstance(final Map<String, Object> config);
}
