package fi.fmi.avi.archiver.util.instantiation;

import static java.util.Objects.requireNonNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

/**
 * A {@code ObjectFactory} constructing and injecting any config properties using Java reflection API.
 *
 * <p>
 * A new factory instance is built with a {@link Builder}, storing constant constructor parameter values, called <em>dependency args</em>, and names and
 * types of configuration options, called <em>config args</em>, in the order of declared constructor parameters. The class to construct
 * must be accessible (public in most cases) and must have a <em>public</em> constructor matching configured constructor parameters.
 * </p>
 *
 * <p>
 * The factory instance name is by default the name provided by {@link ObjectFactory#getName()}, but may be set to custom value in the
 * {@link Builder#setName(String) builder}.
 * </p>
 *
 * @param <T>
 *         target object type this factory constructs
 */
public class ReflectionObjectFactory<T> extends AbstractObjectFactory<T> {
    private final Class<T> type;
    private final ConfigValueConverter configValueConverter;
    private final Object[] constructorArgsTemplate;
    private final Map<String, Integer> configConstructorArgIndicesByName;
    private final Constructor<T> constructor;
    @Nullable
    private final String name;

    private ReflectionObjectFactory(final Builder<T> builder) {
        this.configValueConverter = requireNonNull(builder.configValueConverter, "configValueConverter");
        this.type = requireNonNull(builder.type, "type");
        this.name = builder.name;
        this.constructorArgsTemplate = requireNonNull(builder.constructorArgsTemplate, "constructorArgsTemplate").toArray();
        this.configConstructorArgIndicesByName = Collections.unmodifiableMap(
                new HashMap<>(requireNonNull(builder.configConstructorArgIndicesByName, "configConstructorArgIndicesByName")));
        this.constructor = requireNonNull(builder.constructor, "constructor");

    }

    /**
     * Create a new {@code Builder} for a factory creating instances of provided {@code type}.
     *
     * @param type
     *         type of object this factory will construct
     * @param configValueConverter
     *         configuration value converter
     * @param <T>
     *         type of object this factory will construct
     *
     * @return new builder instance
     */
    public static <T> Builder<T> builder(final Class<T> type, final ConfigValueConverter configValueConverter) {
        return new Builder<>(type, configValueConverter);
    }

    @Override
    protected ConfigValueConverter getConfigValueConverter() {
        return configValueConverter;
    }

    @Override
    protected boolean isInstantiationConfigOption(final String configOptionName) {
        requireNonNull(configOptionName, "configOptionName");
        return configConstructorArgIndicesByName.containsKey(configOptionName);
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
    protected T createInstance(final Map<String, ?> instantiationConfig) {
        requireNonNull(instantiationConfig, "instantiationConfig");
        final Object[] constructorArgs = constructorArgs(instantiationConfig);
        try {
            return constructor.newInstance(constructorArgs);
        } catch (final InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException(String.format(Locale.ROOT, "Unable to construct '%s' (%s)", getName(), type), e);
        } catch (final InvocationTargetException e) {
            throw new IllegalStateException(String.format(Locale.ROOT, "Error while constructing '%s' (%s)", getName(), type), e.getCause());
        }
    }

    private Object[] constructorArgs(final Map<String, ?> config) {
        final Object[] args = constructorArgsTemplate.clone();
        configConstructorArgIndicesByName.forEach((configOptionName, constructorArgIndex) -> {
            final Object configValue = config.get(configOptionName);
            if (configValue == null && !config.containsKey(configOptionName)) {
                throw new IllegalArgumentException(
                        String.format(Locale.ROOT, "Missing required config option [%s] for '%s' (%s)", configOptionName, getName(), type));
            }
            try {
                args[constructorArgIndex] = configValueConverter.toParameterType(configValue, constructor, constructorArgIndex);
            } catch (final RuntimeException e) {
                throw new IllegalArgumentException(
                        String.format(Locale.ROOT, "Unable to convert config option [%s] value for '%s' (%s)", configOptionName, getName(), type), e);
            }
        });
        return args;
    }

    public static class Builder<T> {
        private final Class<T> type;
        private final ConfigValueConverter configValueConverter;

        private final ArrayList<Object> constructorArgsTemplate = new ArrayList<>();
        private final ArrayList<Class<?>> constructorParameterTypes = new ArrayList<>();
        private final Map<String, Integer> configConstructorArgIndicesByName = new HashMap<>();
        private Constructor<T> constructor;

        @Nullable
        private String name;

        private Builder(final Class<T> type, final ConfigValueConverter configValueConverter) {
            this.type = requireNonNull(type, "type");
            this.configValueConverter = requireNonNull(configValueConverter, "configValueConverter");
        }

        private static String listOfClassNames(final List<Class<?>> classes) {
            return classes.stream()//
                    .map(cls -> cls == null ? "null" : cls.getName())//
                    .collect(Collectors.joining(", ", "[", "]"));
        }

        public ReflectionObjectFactory<T> build() {
            assert constructorArgsTemplate.size() == constructorParameterTypes.size() //
                    : "constructorArgsTemplate mismatches constructorParameterTypes in size";
            constructor = resolveConstructor();
            return new ReflectionObjectFactory<>(this);
        }

        private Constructor<T> resolveConstructor() {
            final Iterator<Constructor<?>> constructors = Arrays.stream(type.getConstructors())//
                    .filter(constructor -> areAssignable(constructor.getParameterTypes()))//
                    .iterator();
            if (!constructors.hasNext()) {
                throw new IllegalStateException(String.format(Locale.ROOT, "No suitable public constructor found for [%s] with args assignable from %s", type,
                        listOfClassNames(constructorParameterTypes)));
            }
            @SuppressWarnings("unchecked")
            final Constructor<T> constructor = (Constructor<T>) constructors.next();
            if (constructors.hasNext()) {
                throw new IllegalStateException(String.format(Locale.ROOT, "Multiple constructors found for [%s] with args assignable from %s", type,
                        listOfClassNames(constructorParameterTypes)));
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
            return clearAllArgs()//
                    .clearName();
        }

        public Builder<T> clearAllArgs() {
            clearConstructor();
            constructorArgsTemplate.clear();
            constructorParameterTypes.clear();
            configConstructorArgIndicesByName.clear();
            return this;
        }

        @SuppressWarnings("ConstantConditions")
        private void clearConstructor() {
            constructor = null;
        }

        public Builder<T> addDependencyArgs(final Object... dependencies) {
            requireNonNull(dependencies, "dependencies");
            for (final Object dependency : dependencies) {
                addDependencyArg(dependency);
            }
            return this;
        }

        @SuppressWarnings("unchecked")
        public <D> Builder<T> addDependencyArg(@Nullable final D dependency) {
            return addDependencyArgOfNullableType(dependency, dependency == null ? null : (Class<? super D>) dependency.getClass());
        }

        public <D> Builder<T> addDependencyArg(@Nullable final D dependency, final Class<? super D> type) {
            requireNonNull(type, "type");
            return addDependencyArgOfNullableType(dependency, type);
        }

        private <D> Builder<T> addDependencyArgOfNullableType(@Nullable final D dependency, @Nullable final Class<? super D> type) {
            clearConstructor();
            constructorArgsTemplate.add(dependency);
            constructorParameterTypes.add(type);
            return this;
        }

        public Builder<T> addConfigArg(final String name, final Class<?> type) {
            requireNonNull(name, "name");
            requireNonNull(type, "type");
            if (configConstructorArgIndicesByName.containsKey(name)) {
                throw new IllegalArgumentException(String.format(Locale.ROOT, "Duplicate config arg name [%s]", name));
            }

            clearConstructor();
            final int argumentIndex = constructorArgsTemplate.size();
            configConstructorArgIndicesByName.put(name, argumentIndex);
            constructorArgsTemplate.add(name); // placeholder for config option value
            constructorParameterTypes.add(type);
            return this;
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
    }
}
