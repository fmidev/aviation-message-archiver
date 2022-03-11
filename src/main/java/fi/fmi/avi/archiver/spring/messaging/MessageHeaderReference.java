package fi.fmi.avi.archiver.spring.messaging;

import static java.util.Objects.requireNonNull;

import java.util.Optional;

import javax.annotation.Nullable;

import org.springframework.messaging.MessageHeaders;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class MessageHeaderReference<T> {
    MessageHeaderReference() {
    }

    public static <T> MessageHeaderReference<T> of(final String name, final Class<T> type) {
        return new AutoValue_MessageHeaderReference<>(name, type);
    }

    public static <T> MessageHeaderReference<T> simpleNameOf(final Class<T> type) {
        return of(type.getSimpleName(), type);
    }

    public abstract String getName();

    public abstract Class<T> getType();

    public T getNonNull(final MessageHeaders headers) {
        return requireNonNull(getNullable(headers), getName());
    }

    @Nullable
    public T getNullable(final MessageHeaders headers) {
        requireNonNull(headers, "headers");
        return headers.get(getName(), getType());
    }

    public Optional<T> getOptional(final MessageHeaders headers) {
        return Optional.ofNullable(getNullable(headers));
    }

    @Override
    public String toString() {
        return getName();
    }
}
