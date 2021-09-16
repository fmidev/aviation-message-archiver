package fi.fmi.avi.archiver.message.populator;

import java.util.Map;

public interface MessagePopulatorFactory<T extends MessagePopulator> {
    Class<T> getType();

    default String getName() {
        return getType().getSimpleName();
    }

    T newInstance(final Map<String, Object> arguments, final Map<String, Object> options);
}
