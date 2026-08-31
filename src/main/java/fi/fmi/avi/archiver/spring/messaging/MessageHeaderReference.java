package fi.fmi.avi.archiver.spring.messaging;

import com.google.auto.value.AutoValue;
import org.jspecify.annotations.Nullable;
import org.springframework.messaging.MessageHeaders;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

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


    public @Nullable T getNullable(final MessageHeaders headers) {
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
