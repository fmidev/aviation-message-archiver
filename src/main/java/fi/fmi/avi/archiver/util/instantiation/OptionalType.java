package fi.fmi.avi.archiver.util.instantiation;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public enum OptionalType {
    OPTIONAL(Optional.class, Object.class, Optional::empty) {
        @Override
        public Optional<Class<?>> getValueType(final Type genericOptionalType) {
            requireNonNull(genericOptionalType, "genericOptionalType");
            if (genericOptionalType instanceof ParameterizedType
                    && ((ParameterizedType) genericOptionalType).getRawType() instanceof Class<?>
                    && Optional.class.isAssignableFrom((Class<?>) ((ParameterizedType) genericOptionalType).getRawType())) {
                final Type[] actualTypeArguments = ((ParameterizedType) genericOptionalType).getActualTypeArguments();
                if (actualTypeArguments.length == 1 && actualTypeArguments[0] instanceof Class<?>) {
                    return Optional.of((Class<?>) actualTypeArguments[0]);
                }
            } else if (genericOptionalType instanceof Class<?>
                    && Optional.class.isAssignableFrom((Class<?>) genericOptionalType)) {
                return Optional.of(Object.class);
            }
            return Optional.empty();
        }
    },
    OPTIONAL_INT(OptionalInt.class, int.class, OptionalInt::empty),
    OPTIONAL_LONG(OptionalLong.class, long.class, OptionalLong::empty),
    OPTIONAL_DOUBLE(OptionalDouble.class, double.class, OptionalDouble::empty),
    ;

    private final Class<?> optionalClass;
    private final Class<?> valueType;
    private final Supplier<Object> emptySupplier;

    OptionalType(final Class<?> optionalClass, final Class<?> valueType, final Supplier<Object> emptySupplier) {
        this.optionalClass = requireNonNull(optionalClass, "optionalClass");
        this.valueType = requireNonNull(valueType, "optionalClass");
        this.emptySupplier = requireNonNull(emptySupplier, "emptySupplier");
    }

    static Optional<OptionalType> of(final Type type) {
        return Arrays.stream(values())
                .filter(optionalType -> optionalType.isOptional(type))
                .findAny();
    }

    public static Optional<Class<?>> getAnyValueType(final Type genericOptionalType) {
        return of(genericOptionalType)
                .flatMap(optionalType -> optionalType.getValueType(genericOptionalType));
    }

    public Class<?> getOptionalClass() {
        return optionalClass;
    }

    public boolean isOptional(final Type type) {
        requireNonNull(type, "type");
        return getRawType(type)
                .map(optionalClass::isAssignableFrom)
                .orElse(false);
    }

    private Optional<Class<?>> getRawType(final Type type) {
        if (type instanceof final Class<?> classType) {
            return Optional.of(classType);
        } else if (type instanceof final ParameterizedType parameterizedType) {
            final Type rawType = parameterizedType.getRawType();
            if (rawType instanceof final Class<?> classType) {
                return Optional.of(classType);
            }
        }
        return Optional.empty();
    }

    public Object empty() {
        return emptySupplier.get();
    }

    public Optional<Class<?>> getValueType(final Type genericOptionalType) {
        requireNonNull(genericOptionalType, "genericOptionalType");
        return isOptional(genericOptionalType)
                ? Optional.of(valueType)
                : Optional.empty();
    }
}
