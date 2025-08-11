package fi.fmi.avi.archiver.util.instantiation;

import java.util.Map;

public interface ObjectFactoryConfigFactory {
    <C extends ObjectFactoryConfig> boolean isValidConfigType(Class<C> configType);

    <C extends ObjectFactoryConfig> C create(Class<C> configType, Map<?, ?> sourceMap);
}
