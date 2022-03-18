package fi.fmi.avi.archiver.spring.retry;

import static java.util.Objects.requireNonNull;

import java.util.function.UnaryOperator;

import javax.annotation.Nullable;

import org.inferred.freebuilder.FreeBuilder;
import org.springframework.retry.RetryContext;

@FreeBuilder
public abstract class RetryContextAttributeAccessor<T> {
    RetryContextAttributeAccessor() {
    }

    public static <T> Builder<T> builder(final Class<T> type) {
        return new Builder<T>().setType(type);
    }

    @Nullable
    public final T get(final RetryContext context) {
        requireNonNull(context, "context");
        final Object value = context.getAttribute(getName());
        return getType().isInstance(value) ? getDoOnGet().apply(getType().cast(value)) : getDefaultValue();
    }

    public final void set(final RetryContext context, @Nullable final T value) {
        requireNonNull(context, "context");
        context.setAttribute(getName(), value == null ? null : getDoOnSet().apply(value));
    }

    public final void clear(final RetryContext context) {
        requireNonNull(context, "context");
        context.removeAttribute(getName());
    }

    public abstract String getName();

    public abstract Class<T> getType();

    @Nullable
    public abstract T getDefaultValue();

    abstract UnaryOperator<T> getDoOnGet();

    abstract UnaryOperator<T> getDoOnSet();

    @Override
    public String toString() {
        return getName();
    }

    public static class Builder<T> extends RetryContextAttributeAccessor_Builder<T> {
        Builder() {
            setDoOnGet(UnaryOperator.identity());
            setDoOnSet(UnaryOperator.identity());
        }

        public Builder<T> setNameFromType() {
            return setName(getType().getName());
        }
    }
}
