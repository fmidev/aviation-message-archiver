package fi.fmi.avi.archiver.util.instantiation;

import java.util.Map;

/**
 * A factory interface that instantiates a typed configuration object from provided {@code Map}, possibly converting
 * values from source type to required type.
 *
 * <p>
 * Each configuration type must implement the {@link ObjectFactoryConfig} marker interface. The factory implementation
 * must support the characteristics described in the {@code ObjectFactoryConfig} documentation. The factory
 * implementation may also define further restrictions on supported configuration types.
 * </p>
 */
public interface ObjectFactoryConfigFactory {
    /**
     * Test whether provided {@code configType} is a valid configuration type.
     * Unlike {@link #create(Class, Map)}, which throws an exception on invalid configuration type, this method simply
     * returns a boolean.
     *
     * @param configType configuration type to test
     * @param <C>        configuration type
     * @return a boolean indicating whether provided {@code configType} is a valid configuration type
     */
    <C extends ObjectFactoryConfig> boolean isValidConfigType(Class<C> configType);

    /**
     * Instantiate a typed configuration object of provided {@code configType} from the data of provided
     * {@code sourceMap}.
     * The {@code sourceMap} keys must be convertable to {@code String} and the values must be convertable to
     * corresponding type declared by {@code configType}.
     *
     * @param configType configuration type
     * @param sourceMap  configuration data
     * @param <C>        configuration type
     * @return a typed configuration object
     * @throws IllegalArgumentException if the provided {@code configType} is invalid (thus,
     *                                  {@link #isValidConfigType(Class)} would return {@code false}), {@code sourceMap}
     *                                  does not comply with the configuration type or configuration values cannot be
     *                                  converted to correct type.
     */
    <C extends ObjectFactoryConfig> C create(Class<C> configType, Map<?, ?> sourceMap);
}
