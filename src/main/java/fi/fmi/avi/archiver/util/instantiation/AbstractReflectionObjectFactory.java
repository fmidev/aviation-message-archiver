package fi.fmi.avi.archiver.util.instantiation;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * A skeletal {@code ObjectFactory} implementation.
 * It separates config entries to instantiation config (constructor parameters) and property config (bean setter parameters). After a new instance is
 * created, it sets all property config values by invoking corresponding setters via reflection, converting config values to appropriate types.
 *
 * @param <T> type of objects this factory produces
 */
public abstract class AbstractReflectionObjectFactory<T> implements ObjectFactory<T> {
    private static String classString(final Object object) {
        return object == null ? "null" : object.getClass().toString();
    }

    /**
     * Provides a converter implementation for automatic conversion of property values.
     *
     * @return converter
     */
    protected abstract ConfigValueConverter getConfigValueConverter();

    /**
     * Creates a new object instance.
     *
     * @param instantiationConfig instantiation config values flagged by {@link #isInstantiationConfigOption(String)}.
     * @return a new object instance
     */
    protected abstract T createInstance(final Map<String, ?> instantiationConfig);

    /**
     * Returns {@code true} if provided configuration option is required for instantiation. Otherwise, returns {@code false}, denoting that provided
     * configuration option value is injected using a setter.
     *
     * <p>
     * This default implementation returns always {@code false}. Subclasses may override this method and provide own rules to distinguish instantiation
     * configuration options from property options.
     * </p>
     *
     * @param configOptionName name of configuration option
     * @return {@code true} if provided configuration option is required for instantiation, {@code false} otherwise
     */
    protected boolean isInstantiationConfigOption(final String configOptionName) {
        requireNonNull(configOptionName, "configOptionName");
        return false;
    }

    /**
     * Create a new object instance, applying provided configuration.
     * Typically there is no need ot override this method.
     *
     * @param config configuration
     * @return new configured instance
     */
    @Override
    public T newInstance(final Map<String, Object> config) {
        requireNonNull(config, "config");

        final Map<Boolean, Map<String, Object>> partitionedConfig = config.entrySet().stream()//
                .collect(Collectors.partitioningBy(entry -> isInstantiationConfigOption(entry.getKey()), //
                        Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue)));
        final Map<String, Object> instantiationConfig = partitionedConfig.getOrDefault(true, Collections.emptyMap());
        final Map<String, Object> propertyConfig = partitionedConfig.getOrDefault(false, Collections.emptyMap());

        final T instance = createInstance(instantiationConfig);
        applyPropertyConfig(instance, propertyConfig);
        return instance;
    }

    protected void applyPropertyConfig(final T instance, final Map<String, Object> propertyConfig) {
        requireNonNull(instance, "instance");
        requireNonNull(propertyConfig, "propertyConfig");
        final Map<String, Method> setters = getSettersByName(getType());
        propertyConfig.forEach((propertyName, propertyConfigValue) -> setConfigProperty(propertyName, propertyConfigValue, instance, setters));
    }

    private void setConfigProperty(final String propertyName, final Object propertyConfigValue, final T instance, final Map<String, Method> setters) {
        final Method setterMethod = setters.get(BeanMethodNamePrefix.SET.prefix(propertyName));
        if (setterMethod == null) {
            throw new IllegalArgumentException(String.format(Locale.ROOT, "Unknown config option '%s' for '%s' (%s)", propertyName, getName(), getType()));
        }
        final Object propertyValue;
        try {
            propertyValue = getConfigValueConverter().toParameterType(propertyConfigValue, setterMethod, 0);
        } catch (final RuntimeException e) {
            throw new IllegalStateException(
                    String.format(Locale.ROOT, "Unable to convert '%s' (%s) config option '%s' value of from [%s] to [%s]", getName(), getType(), propertyName,
                            classString(propertyConfigValue), setterMethod.getParameterTypes()[0]), e);
        }
        try {
            setterMethod.invoke(instance, propertyValue);
        } catch (final IllegalAccessException e) {
            throw new IllegalStateException(String.format(Locale.ROOT, "Unable to set '%s' (%s) config option '%s'", getName(), getType(), propertyName), e);
        } catch (final InvocationTargetException e) {
            throw new IllegalStateException(String.format(Locale.ROOT, "Error while setting '%s' (%s) config option '%s'", getName(), getType(), propertyName),
                    e.getCause());
        }
    }

    private Map<String, Method> getSettersByName(final Class<T> beanClass) {
        return Arrays.stream(beanClass.getMethods())//
                .filter(this::isSetter)//
                .collect(Collectors.toUnmodifiableMap(Method::getName, Function.identity()));
    }

    private boolean isSetter(final Method method) {
        final String name = method.getName();
        final int modifiers = method.getModifiers();
        return !Modifier.isStatic(modifiers) //
                && method.getParameterCount() == 1 //
                && BeanMethodNamePrefix.SET.isPrefixed(name);
    }

}
