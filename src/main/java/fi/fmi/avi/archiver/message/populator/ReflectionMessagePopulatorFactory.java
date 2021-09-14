package fi.fmi.avi.archiver.message.populator;

import static java.util.Objects.requireNonNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ReflectionMessagePopulatorFactory<T extends MessagePopulator> extends AbstractMessagePopulatorFactory<T> {
    private final PropertyConverter propertyConverter;
    private final Class<T> type;
    private final Object[] constructorArgs;
    private final Constructor<T> constructor;

    public ReflectionMessagePopulatorFactory(final PropertyConverter propertyConverter, final Class<T> type, final Object... constructorArgs) {
        this.propertyConverter = requireNonNull(propertyConverter, "propertyConverter");
        this.type = requireNonNull(type, "type");
        this.constructorArgs = requireNonNull(constructorArgs, "constructorArgs").clone();
        this.constructor = resolveConstructor(type, constructorArgs);
    }

    private static String listOfClasses(final Object... objects) {
        return Arrays.stream(objects)//
                .map(obj -> obj == null ? "null" : obj.getClass().toString())//
                .collect(Collectors.joining(", ", "[", "]"));
    }

    private Constructor<T> resolveConstructor(final Class<T> type, final Object[] constructorArgs) {
        final List<Constructor<?>> constructors = Arrays.stream(type.getConstructors())//
                .filter(constructor -> areAssignable(constructor.getParameterTypes(), constructorArgs))//
                .collect(Collectors.toList());
        if (constructors.isEmpty()) {
            throw new IllegalArgumentException("No suitable constructor found for type " + type + " with args " + listOfClasses(constructorArgs));
        }
        if (constructors.size() > 1) {
            throw new IllegalArgumentException("Multiple constructors found for type " + type + " with args " + listOfClasses(constructorArgs));
        }
        //noinspection unchecked
        return (Constructor<T>) constructors.get(0);
    }

    private boolean areAssignable(final Class<?>[] classes, final Object[] objects) {
        if (classes.length != objects.length) {
            return false;
        }
        for (int i = 0; i < classes.length; i++) {
            if (!classes[i].isInstance(objects[i])) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected PropertyConverter getPropertyConverter() {
        return propertyConverter;
    }

    @Override
    public Class<T> getType() {
        return type;
    }

    @Override
    public T newInstance() {
        try {
            return constructor.newInstance(constructorArgs);
        } catch (final InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException("Unable to construct " + type, e);
        } catch (final InvocationTargetException e) {
            throw new IllegalStateException("Error while constructing " + type, e.getCause());
        }
    }
}
