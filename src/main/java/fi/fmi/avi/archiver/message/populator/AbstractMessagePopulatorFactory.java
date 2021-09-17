package fi.fmi.avi.archiver.message.populator;

import static java.util.Objects.requireNonNull;

import java.lang.reflect.Executable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

public abstract class AbstractMessagePopulatorFactory<T extends MessagePopulator> implements MessagePopulatorFactory<T> {
    private static String classString(final Object object) {
        return object == null ? "null" : object.getClass().toString();
    }

    protected abstract ConfigValueConverter getConfigValueConverter();

    protected abstract T createInstance(final Map<String, ?> instantiationConfig);

    protected boolean isInstantiationConfigOption(final String configOptionName) {
        requireNonNull(configOptionName, "configOptionName");
        return false;
    }

    @Override
    public T newInstance(final Map<String, Object> config) {
        requireNonNull(config, "config");

        final Map<Boolean, Map<String, Object>> partitionedConfig = config.entrySet().stream()//
                .collect(Collectors.partitioningBy(entry -> isInstantiationConfigOption(entry.getKey()), //
                        Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
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
        final Method setterMethod = setters.get(setterName(propertyName));
        if (setterMethod == null) {
            throw new IllegalArgumentException(
                    String.format(Locale.ROOT, "Unknown config option '%s' for MessagePopulator '%s' (%s)", propertyName, getName(), getType()));
        }
        final Object propertyValue;
        try {
            propertyValue = getConfigValueConverter().convert(propertyConfigValue, setterMethod, 0);
        } catch (final RuntimeException e) {
            throw new IllegalStateException(
                    String.format(Locale.ROOT, "Unable to convert MessagePopulator '%s' (%s) config option '%s' value of from [%s] to [%s]", getName(),
                            getType(), propertyName, classString(propertyConfigValue), setterMethod.getParameterTypes()[0]), e);
        }
        try {
            setterMethod.invoke(instance, propertyValue);
        } catch (final IllegalAccessException e) {
            throw new IllegalStateException(
                    String.format(Locale.ROOT, "Unable to set MessagePopulator '%s' (%s) config option '%s'", getName(), getType(), propertyName), e);
        } catch (final InvocationTargetException e) {
            throw new IllegalStateException(
                    String.format(Locale.ROOT, "Error while setting MessagePopulator '%s' (%s) config option '%s'", getName(), getType(), propertyName),
                    e.getCause());
        }
    }

    private Map<String, Method> getSettersByName(final Class<T> beanClass) {
        return Arrays.stream(beanClass.getMethods())//
                .filter(this::isSetter)//
                .collect(Collectors.toMap(Method::getName, Function.identity()));
    }

    private boolean isSetter(final Method method) {
        final String name = method.getName();
        final int modifiers = method.getModifiers();
        return !Modifier.isStatic(modifiers) //
                && method.getParameterCount() == 1 //
                && name.length() > 3 //
                && name.startsWith("set")//
                && Character.isUpperCase(name.charAt(3));
    }

    private String setterName(final String propertyName) {
        return "set" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
    }

    @FunctionalInterface
    public interface ConfigValueConverter {
        @Nullable
        Object convert(@Nullable final Object propertyConfigValue, final Executable targetExecutable, final int parameterIndex);
    }
}
