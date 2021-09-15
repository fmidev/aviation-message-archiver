package fi.fmi.avi.archiver.message.populator;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

public abstract class AbstractMessagePopulatorFactory<T extends MessagePopulator> implements MessagePopulatorFactory<T> {
    private static String classString(final Object object) {
        return object == null ? "null" : object.getClass().toString();
    }

    protected abstract PropertyConverter getPropertyConverter();

    protected abstract T newInstance();

    @Override
    public T newInstance(final Map<String, Object> config) {
        final T instance = newInstance();
        configure(instance, config);
        return instance;
    }

    private void configure(final T instance, final Map<String, Object> config) {
        final Map<String, Method> setters = getSettersByName(getType());
        config.forEach((propertyName, propertyConfigValue) -> setProperty(propertyName, propertyConfigValue, instance, setters));
    }

    private void setProperty(final String propertyName, final Object propertyConfigValue, final T instance, final Map<String, Method> setters) {
        final Method setterMethod = setters.get(setterName(propertyName));
        if (setterMethod == null) {
            throw new IllegalArgumentException(String.format(Locale.ROOT, "Unknown property '%s' for [%s]", propertyName, getType()));
        }
        final Object propertyValue;
        try {
            propertyValue = getPropertyConverter().convert(propertyConfigValue, setterMethod);
        } catch (final RuntimeException e) {
            throw new IllegalStateException(
                    String.format(Locale.ROOT, "Unable to convert [%s] property '%s' value of from [%s] to [%s]", getType(), propertyName,
                            classString(propertyConfigValue), setterMethod.getParameterTypes()[0]), e);
        }
        try {
            setterMethod.invoke(instance, propertyValue);
        } catch (final IllegalAccessException e) {
            throw new IllegalStateException(String.format(Locale.ROOT, "Unable to set [%s] property '%s'", getType(), propertyName), e);
        } catch (final InvocationTargetException e) {
            throw new IllegalStateException(String.format(Locale.ROOT, "Error while setting [%s] property '%s'", getType(), propertyName), e.getCause());
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
    public interface PropertyConverter {
        @Nullable
        Object convert(@Nullable final Object propertyConfigValue, final Method setterMethod);
    }
}
