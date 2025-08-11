package fi.fmi.avi.archiver.util.instantiation;

import com.google.common.annotations.VisibleForTesting;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

/**
 * An implementation of {@link ObjectFactoryConfigFactory} that creates instances of {@link ObjectFactoryConfig}
 * interfaces as {@link Proxy} objects, backed by provided {@link Map} of configuration keys and values.
 *
 * <p>
 * The generated proxy objects have a proper implementation for {@link Object#equals(Object)},
 * {@link Object#hashCode()} and {@link Object#toString()}. Objects created by this factory with equal config type and
 * config values are considered equal.
 * </p>
 */
public class ProxyObjectFactoryConfigFactory implements ObjectFactoryConfigFactory {
    private final ConfigValueConverter configValueConverter;

    public ProxyObjectFactoryConfigFactory(final ConfigValueConverter configValueConverter) {
        this.configValueConverter = requireNonNull(configValueConverter, "configValueConverter");
    }

    private static boolean isConfigPropertyMethod(final Method method) {
        return Modifier.isAbstract(method.getModifiers());
    }

    @Override
    public <C extends ObjectFactoryConfig> boolean isValidConfigType(final Class<C> configType) {
        return validateConfigClass(configType, new Context<>(false, true));
    }

    private <T> boolean validateConfigClass(final Class<T> type, final Context<Class<?>> context) {
        try (final Context.CircularityDetection ignored = context.doWith(type)) {
            if (!type.isInterface()) {
                return context.throwOrFalse("%s must be an interface", type);
            }
            if (!ObjectFactoryConfig.class.isAssignableFrom(type)) {
                return context.throwOrFalse("%s must extend %s", type, ObjectFactoryConfig.class);
            }
            return validateConfigClassMethods(type, context);
        } catch (final CircularityException exception) {
            return context.throwOrFalse(exception);
        }
    }

    private <T> boolean validateConfigClassMethods(final Class<T> type, final Context<Class<?>> context) {
        final List<Method> configPropertyMethods = getConfigPropertyMethods(type).toList();
        if (!configPropertyMethods.stream().allMatch(method -> validateConfigPropertyMethod(method, context))) {
            return false;
        }
        final List<String> ambiguousMethodNames = configPropertyMethods.stream()
                .map(Method::getName)
                .collect(Collectors.groupingBy(BeanMethodNamePrefix::stripAny))
                .values()
                .stream()
                .filter(methodNames -> methodNames.size() > 1)
                .findAny()
                .orElse(List.of());
        if (!ambiguousMethodNames.isEmpty()) {
            return context.throwOrFalse("Ambiguous config method names: %s", ambiguousMethodNames);
        }
        return true;
    }

    private <T> Stream<Method> getConfigPropertyMethods(final Class<T> type) {
        return Arrays.stream(type.getMethods())
                .filter(ProxyObjectFactoryConfigFactory::isConfigPropertyMethod);
    }

    private boolean validateConfigPropertyMethod(final Method method, final Context<Class<?>> context) {
        if (method.getParameterCount() != 0) {
            return context.throwOrFalse("%s must not have parameters", method);
        }
        final Class<?> unwrappedReturnType = OptionalType.getAnyValueType(method.getGenericReturnType())
                .orElse(method.getReturnType());
        return !context.isValidateRecursively()
                || !ObjectFactoryConfig.class.isAssignableFrom(unwrappedReturnType)
                || validateConfigClass(unwrappedReturnType, context);
    }

    @Override
    public <C extends ObjectFactoryConfig> C create(final Class<C> configType, final Map<?, ?> sourceMap) {
        requireNonNull(sourceMap, "sourceMap");
        requireNonNull(configType, "configType");
        return create(configType, sourceMap, new Context<>(true, false));
    }

    private <C extends ObjectFactoryConfig> C create(final Class<C> configType, final Map<?, ?> sourceMap, final Context<Class<?>> context) {
        validateConfigClass(configType, context);
        try (final Context.CircularityDetection ignored = context.doWith(configType)) {
            return createProxy(configType, convertMapForProxy(configType, sourceMap, context));
        } catch (final CircularityException exception) {
            throw context.doThrow(exception);
        }
    }

    private <C extends ObjectFactoryConfig> Map<String, Object> convertMapForProxy(
            final Class<C> configType, final Map<?, ?> sourceMap, final Context<Class<?>> context) {
        final Map<String, Method> methodsByPropertyName = getConfigPropertyMethods(configType)
                .collect(Collectors.toUnmodifiableMap(
                        method -> BeanMethodNamePrefix.stripAny(method.getName()),
                        Function.identity()
                ));
        final Map<String, ?> sourceMapWithStringKeys = sourceMap.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(entry -> {
                            final String key = requireNonNull(entry.getKey(), "null key").toString();
                            if (!methodsByPropertyName.containsKey(key)) {
                                throw context.exception("Unknown config option '%s'", entry.getKey());
                            }
                            return key;
                        },
                        Map.Entry::getValue
                ));
        return methodsByPropertyName.values().stream()
                .collect(Collectors.toUnmodifiableMap(Method::getName, method -> {
                            final String propertyName = BeanMethodNamePrefix.stripAny(method.getName());
                            return Optional.ofNullable(sourceMapWithStringKeys.get(propertyName))
                                    .map(value -> convertToMethodReturnType(propertyName, method, value, context))
                                    .orElseGet(() -> OptionalType.of(method.getReturnType())
                                            .map(OptionalType::empty)
                                            .orElseThrow(() -> context.exception(
                                                    "Missing required config option [%s]", propertyName)));
                        }
                ));
    }

    private Object convertToMethodReturnType(final Object propertyName, final Method method, final Object value, final Context<Class<?>> context) {
        final Class<?> unwrappedReturnType = OptionalType.getAnyValueType(method.getGenericReturnType())
                .orElse(method.getReturnType());
        if (ObjectFactoryConfig.class.isAssignableFrom(unwrappedReturnType) && value instanceof Map) {
            //noinspection unchecked
            return create((Class<? extends ObjectFactoryConfig>) unwrappedReturnType, (Map<?, ?>) value, context);
        } else {
            try {
                return configValueConverter.toReturnValueType(value, method);
            } catch (final ConfigValueConversionException exception) {
                throw new IllegalArgumentException("Failed to resolve value for config option [%s]: %s".formatted(propertyName, exception.getMessage()), exception);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <C extends ObjectFactoryConfig> C createProxy(final Class<C> configType, final Map<String, Object> configMap) {
        return (C) Proxy.newProxyInstance(
                configType.getClassLoader(),
                new Class[]{configType},
                new ConfigInvocationHandler<>(configType, configMap)
        );
    }

    private static class Context<K> {
        private final boolean failOnInvalid;
        private final boolean validateRecursively;
        private final List<K> seen = new ArrayList<>();

        public Context(final boolean failOnInvalid, final boolean validateRecursively) {
            this.failOnInvalid = failOnInvalid;
            this.validateRecursively = validateRecursively;
        }

        public boolean isValidateRecursively() {
            return validateRecursively;
        }

        public boolean throwOrFalse(final String message, final Object... messageArgs) {
            if (failOnInvalid) {
                throw new IllegalArgumentException(message.formatted(messageArgs));
            } else {
                return false;
            }
        }

        public boolean throwOrFalse(final CircularityException circularityException) {
            return throwOrFalse(circularityException.getMessage());
        }

        public IllegalArgumentException exception(final String message, final Object... messageArgs) {
            return new IllegalArgumentException(message.formatted(messageArgs));
        }

        public IllegalArgumentException doThrow(final String message, final Object... messageArgs) {
            throw exception(message.formatted(messageArgs));
        }

        public IllegalArgumentException doThrow(final CircularityException circularityException) {
            return doThrow(circularityException.getMessage());
        }

        public CircularityDetection doWith(final K key) throws CircularityException {
            if (seen.contains(key)) {
                throw new CircularityException("Circular property path detected: %s -> %s".formatted(seen, key));
            }
            seen.add(key);
            return seen::removeLast;
        }

        @FunctionalInterface
        public interface CircularityDetection extends AutoCloseable {
            @Override
            void close();
        }
    }

    private static class CircularityException extends Exception {
        public CircularityException(final String message) {
            super(message);
        }
    }

    @VisibleForTesting
    record ConfigInvocationHandler<C extends ObjectFactoryConfig>(
            Class<C> configType,
            Map<String, Object> configMap
    ) implements InvocationHandler {
        public static final String STRING_ENTRY_SEPARATOR = ", ";

        @Override
        public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
            if (isConfigPropertyMethod(method)) {
                return configMap.get(method.getName());
            } else if (method.isDefault()) {
                return InvocationHandler.invokeDefault(proxy, method, args);
            } else if (Modifier.isStatic(method.getModifiers())) {
                return method.invoke(null, args);
            } else if (method.getDeclaringClass().isAssignableFrom(Object.class)) {
                return switch (method.getName()) {
                    case "equals" -> proxyEquals(proxy, args[0]);
                    case "hashCode" -> hashCode();
                    case "toString" -> proxyToString();
                    default -> null;
                };
            }
            throw new UnsupportedOperationException("Unsupported config class method: " + method);
        }

        private String proxyToString() {
            final StringBuilder builder = new StringBuilder();
            builder.append(configType.getSimpleName())
                    .append('{');
            configMap.forEach((key, value) -> builder.append(BeanMethodNamePrefix.stripAny(key))
                    .append("=")
                    .append(value)
                    .append(STRING_ENTRY_SEPARATOR));
            if (!configMap.isEmpty()) {
                builder.setLength(builder.length() - STRING_ENTRY_SEPARATOR.length());
            }
            builder.append('}');
            return builder.toString();
        }

        private boolean proxyEquals(final Object proxy, final Object other) {
            return proxy == other || (
                    other != null
                            && Proxy.isProxyClass(other.getClass())
                            && this.equals(Proxy.getInvocationHandler(other)));
        }
    }
}
