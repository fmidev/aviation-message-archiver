package fi.fmi.avi.archiver.message.populator;

import static java.util.Objects.requireNonNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

public class ReflectionMessagePopulatorFactory<T extends MessagePopulator> extends AbstractMessagePopulatorFactory<T> {
    private final Class<T> type;
    private final PropertyConverter propertyConverter;
    private final Object[] dependenciesAndArgumentNames;
    private final BitSet argumentIndices;
    private final Set<String> argumentNames;
    private final Constructor<T> constructor;
    @Nullable
    private final String name;

    private ReflectionMessagePopulatorFactory(final Builder<T> builder) {
        this.propertyConverter = requireNonNull(builder.propertyConverter, "propertyConverter");
        this.type = requireNonNull(builder.type, "type");
        this.name = builder.name;
        this.dependenciesAndArgumentNames = requireNonNull(builder.dependenciesAndArgumentNames, "dependenciesAndArgumentNames").toArray();
        this.argumentIndices = (BitSet) requireNonNull(builder.argumentIndices, "argumentIndices").clone();
        this.constructor = requireNonNull(builder.constructor, "constructor");

        this.argumentNames = Collections.unmodifiableSet(argumentIndices.stream()//
                .mapToObj(i -> dependenciesAndArgumentNames[i].toString())//
                .collect(Collectors.toSet()));
    }

    public static <T extends MessagePopulator> Builder<T> builder(final Class<T> type, final PropertyConverter propertyConverter) {
        return new Builder<>(type, propertyConverter);
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
    public String getName() {
        return name == null ? super.getName() : name;
    }

    @Override
    public T newInstance(final Map<String, ?> arguments) {
        try {
            return constructor.newInstance(constructorArgs(arguments));
        } catch (final InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException("Unable to construct MessagePopulator '" + getName() + "' (" + type + ')', e);
        } catch (final InvocationTargetException e) {
            throw new IllegalStateException("Error while constructing MessagePopulator '" + getName() + "' (" + type + ')', e.getCause());
        }
    }

    private Object[] constructorArgs(final Map<String, ?> arguments) {
        final Object[] args = dependenciesAndArgumentNames.clone();
        argumentIndices.stream().forEach(i -> {
            final String argumentName = args[i].toString();
            if (!arguments.containsKey(argumentName)) {
                throw new IllegalArgumentException("Missing argument [" + argumentName + "] MessagePopulator '" + getName() + '\'');
            }
            args[i] = propertyConverter.convert(arguments.get(argumentName), constructor, i);
        });
        if (arguments.size() > argumentNames.size()) {
            final String unknownArguments = arguments.keySet().stream()//
                    .filter(name -> !argumentNames.contains(name))//
                    .collect(Collectors.joining(",", "[", "]"));
            throw new IllegalArgumentException("Unknown argument(s) " + unknownArguments + " for MessagePopulator '" + getName() + '\'');
        }
        return args;
    }

    public static class Builder<T extends MessagePopulator> {
        private final Class<T> type;
        private final PropertyConverter propertyConverter;

        private final ArrayList<Object> dependenciesAndArgumentNames = new ArrayList<>();
        private final ArrayList<Class<?>> constructorParameterTypes = new ArrayList<>();
        private final BitSet argumentIndices = new BitSet();
        private Constructor<T> constructor;

        @Nullable
        private String name;

        private Builder(final Class<T> type, final PropertyConverter propertyConverter) {
            this.type = requireNonNull(type, "type");
            this.propertyConverter = requireNonNull(propertyConverter, "propertyConverter");
        }

        private static String listOfClassNames(final List<Class<?>> classes) {
            return classes.stream()//
                    .map(cls -> cls == null ? "null" : cls.getName())//
                    .collect(Collectors.joining(", ", "[", "]"));
        }

        public ReflectionMessagePopulatorFactory<T> build() {
            constructor = resolveConstructor();
            return new ReflectionMessagePopulatorFactory<>(this);
        }

        private Constructor<T> resolveConstructor() {
            final Iterator<Constructor<?>> constructors = Arrays.stream(type.getConstructors())//
                    .filter(constructor -> areAssignable(constructor.getParameterTypes()))//
                    .iterator();
            if (!constructors.hasNext()) {
                throw new IllegalStateException(
                        "No suitable public constructor found for " + type + " with args assignable from " + listOfClassNames(constructorParameterTypes));
            }
            @SuppressWarnings("unchecked")
            final Constructor<T> constructor = (Constructor<T>) constructors.next();
            if (constructors.hasNext()) {
                throw new IllegalStateException(
                        "Multiple constructors found for " + type + " with args assignable from " + listOfClassNames(constructorParameterTypes));
            }
            return constructor;
        }

        private boolean areAssignable(final Class<?>[] classes) {
            if (classes.length != constructorParameterTypes.size()) {
                return false;
            }
            for (int i = 0; i < classes.length; i++) {
                @Nullable
                final Class<?> parameterType = constructorParameterTypes.get(i);
                if (parameterType != null && !classes[i].isAssignableFrom(parameterType)) {
                    return false;
                }
            }
            return true;
        }

        public Builder<T> clear() {
            return clearConstructorParameters()//
                    .clearName();
        }

        public Builder<T> clearName() {
            return setNullableName(null);
        }

        public Builder<T> setName(final String name) {
            return setNullableName(requireNonNull(name, "name"));
        }

        public Builder<T> setNullableName(@Nullable final String name) {
            this.name = name;
            return this;
        }

        public Builder<T> clearConstructorParameters() {
            clearConstructor();
            dependenciesAndArgumentNames.clear();
            constructorParameterTypes.clear();
            argumentIndices.clear();
            return this;
        }

        @SuppressWarnings("ConstantConditions")
        private void clearConstructor() {
            constructor = null;
        }

        public Builder<T> addDependencies(final Object... dependencies) {
            requireNonNull(dependencies, "dependencies");
            for (final Object dependency : dependencies) {
                addDependency(dependency);
            }
            return this;
        }

        @SuppressWarnings("unchecked")
        public <D> Builder<T> addDependency(@Nullable final D dependency) {
            return addDependencyOfNullableType(dependency, dependency == null ? null : (Class<? super D>) dependency.getClass());
        }

        public <D> Builder<T> addDependency(@Nullable final D dependency, final Class<? super D> type) {
            requireNonNull(type, "type");
            return addDependencyOfNullableType(dependency, type);
        }

        private <D> Builder<T> addDependencyOfNullableType(@Nullable final D dependency, @Nullable final Class<? super D> type) {
            clearConstructor();
            dependenciesAndArgumentNames.add(dependency);
            constructorParameterTypes.add(type);
            return this;
        }

        public Builder<T> addArgument(final String name, final Class<?> type) {
            requireNonNull(name, "name");
            requireNonNull(type, "type");
            clearConstructor();
            final int argumentIndex = dependenciesAndArgumentNames.size();
            argumentIndices.set(argumentIndex);
            dependenciesAndArgumentNames.add(name);
            constructorParameterTypes.add(type);
            return this;
        }
    }
}
