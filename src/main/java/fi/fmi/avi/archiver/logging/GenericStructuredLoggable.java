package fi.fmi.avi.archiver.logging;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.util.StdConverter;
import com.google.auto.value.AutoValue;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/**
 * A generic {@link StructuredLoggable} implementation, that can be used to create ad-hoc instances for any values.
 * It holds the log message string and an object value that this object serializes to when serializing to JSON.
 * Either or both value and string may be evaluated lazily at logging time to avoid extra overhead of an
 * expensive construction when the value is not going to be logged.
 * Lazy evaluation takes place on each invocation to {@link #getValue()} and {@link #toString()}.
 *
 * @param <T> type of value object
 */
@JsonSerialize(converter = GenericStructuredLoggable.ToValueConverter.class)
public abstract class GenericStructuredLoggable<T> extends AbstractLoggable implements StructuredLoggable {
    GenericStructuredLoggable() {
    }

    /**
     * Return an instance that evaluates value and string lazily.
     *
     * @param structureName  structure name
     * @param valueSupplier  supplier providing the value object
     * @param stringSupplier supplier providing the log string
     * @param <T>            value type
     * @return lazily evaluating loggable instance
     */
    public static <T> GenericStructuredLoggable<T> loggable(final String structureName, final Supplier<@Nullable T> valueSupplier,
                                                            final Supplier<String> stringSupplier) {
        //noinspection NullableProblems - AutoValue doesn't seem to support @Nullable type parameters yet (used in valueSupplier type)
        return new AutoValue_GenericStructuredLoggable_LazyValue<>(structureName, valueSupplier, stringSupplier);
    }

    /**
     * Return an immutable instance holding provided {@code value} and {@code string}.
     *
     * @param structureName structure name
     * @param value         the value object
     * @param string        the log string
     * @param <T>           value type
     * @return immutable loggable instance
     */
    public static <T> GenericStructuredLoggable<T> loggable(final String structureName, final @Nullable T value, final String string) {
        return new AutoValue_GenericStructuredLoggable_ImmutableValue<>(structureName, value, string);
    }

    /**
     * Return an instance that returns the lazily evaluated log string as value.
     *
     * @param structureName  structure name
     * @param stringSupplier supplier providing the log string
     * @return lazily evaluating loggable instance
     */
    public static GenericStructuredLoggable<String> loggableString(final String structureName, final Supplier<String> stringSupplier) {
        return loggable(structureName, stringSupplier, stringSupplier);
    }

    /**
     * Return an immutable instance that returns the provided log string as value.
     *
     * @param structureName structure name
     * @param string        the log string
     * @return immutable loggable instance
     */
    public static GenericStructuredLoggable<String> loggableString(final String structureName, final String string) {
        requireNonNull(string, "string");
        return loggable(structureName, string, string);
    }

    /**
     * Return an instance that lazily evaluates value, and log string from value string.
     * Note that value will be evaluated <em>also</em> on {@link #toString()} invocation.
     *
     * @param structureName structure name
     * @param valueSupplier supplier providing the value object
     * @param <T>           value type
     * @return lazily evaluating loggable instance
     */
    public static <T> GenericStructuredLoggable<T> loggableValue(final String structureName, final Supplier<@Nullable T> valueSupplier) {
        return loggable(structureName, valueSupplier, () -> String.valueOf(valueSupplier.get()));
    }

    /**
     * Return an instance that lazily evaluates the log string from provided value.
     *
     * @param structureName structure name
     * @param value         the value object
     * @param <T>           value type
     * @return lazily evaluating loggable instance
     */
    public static <T> GenericStructuredLoggable<T> loggableValue(final String structureName, final @Nullable T value) {
        return loggable(structureName, () -> value, () -> String.valueOf(value));
    }

    /**
     * Return the value object.
     *
     * @return the value object
     */
    public abstract @Nullable T getValue();

    /**
     * Return an immutable copy of this instance.
     * Instances that evaluate value and/or string lazily, evaluates values and returns an immutable copy.
     * Already immutable instances return the same instance.
     *
     * @return this object as immutable
     */
    @Override
    public abstract GenericStructuredLoggable<T> readableCopy();

    @AutoValue
    static abstract class LazyValue<T> extends GenericStructuredLoggable<T> {
        LazyValue() {
        }

        abstract Supplier<@Nullable T> getValueSupplier();

        abstract Supplier<String> getStringSupplier();

        @Override
        public int estimateLogStringLength() {
            return 0;
        }

        @Override
        public GenericStructuredLoggable<T> readableCopy() {
            return loggable(getStructureName(), getValue(), toString());
        }

        @Override
        public @Nullable T getValue() {
            return getValueSupplier().get();
        }

        @Override
        public String toString() {
            //noinspection UnnecessaryCallToStringValueOf: ensure null value does not interfere execution
            return String.valueOf(getStringSupplier().get());
        }
    }

    @AutoValue
    static abstract class ImmutableValue<T> extends GenericStructuredLoggable<T> {
        ImmutableValue() {
        }

        abstract String getString();

        @Override
        public int estimateLogStringLength() {
            return toString().length();
        }

        @Override
        public GenericStructuredLoggable<T> readableCopy() {
            return this;
        }

        @Override
        public String toString() {
            return getString();
        }
    }

    static class ToValueConverter<T> extends StdConverter<GenericStructuredLoggable<T>, T> {
        @Override
        public @Nullable T convert(final GenericStructuredLoggable<T> value) {
            return value.getValue();
        }
    }
}
