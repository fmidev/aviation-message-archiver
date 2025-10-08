package fi.fmi.avi.archiver.message.processor;

import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.file.InputBulletinHeading;
import fi.fmi.avi.archiver.message.processor.populator.BulletinHeadingSource;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Function;
import java.util.function.ToIntFunction;

import static java.util.Objects.requireNonNull;

public final class MessageProcessorHelper {
    private MessageProcessorHelper() {
        throw new AssertionError();
    }

    /**
     * Attempt to read a mandatory value from a FreeBuilder builder class that may not yet have been set.
     * This method catches the {@link IllegalStateException} possibly thrown by the property getter and returns an empty Optional.
     *
     * @param input  input builder
     * @param reader function to read a property from the provided {@code builder}
     * @param <F>    builder type
     * @param <T>    return value type
     * @return value returned by {@code reader} or {@link Optional#empty()} if value could not be read
     */
    public static <F, T> Optional<T> tryGet(final F input, final Function<F, T> reader) {
        try {
            return Optional.ofNullable(reader.apply(input));
        } catch (final IllegalStateException ignored) {
            return Optional.empty();
        }
    }

    /**
     * Attempt to read a mandatory integer value from a FreeBuilder builder class that may not yet have been set.
     * This method catches the {@link IllegalStateException} possibly thrown by the property getter and returns an empty OptionalInt.
     *
     * @param input  input builder
     * @param reader function to read an integer property from the provided {@code builder}
     * @param <F>    builder type
     * @return value returned by {@code reader} or {@link OptionalInt#empty()} if value could not be read
     */
    public static <F> OptionalInt tryGetInt(final F input, final ToIntFunction<F> reader) {
        try {
            return OptionalInt.of(reader.applyAsInt(input));
        } catch (final IllegalStateException ignored) {
            return OptionalInt.empty();
        }
    }

    /**
     * Get the first non-null value from the input aviation message's bulletin heading using the given bulletin heading sources.
     *
     * @param bulletinHeadingSources bulletin heading sources
     * @param input                  input aviation message
     * @param fn                     function to get an optional value from the input heading
     * @param <T>                    return value type
     * @return non-null value if present
     */
    public static <T> Optional<T> getFirstNonNullFromBulletinHeading(final Collection<BulletinHeadingSource> bulletinHeadingSources,
                                                                     final InputAviationMessage input, final Function<InputBulletinHeading, Optional<T>> fn) {
        requireNonNull(bulletinHeadingSources, "bulletinHeadingSources");
        requireNonNull(input, "input");
        requireNonNull(fn, "fn");
        return bulletinHeadingSources.stream()
                .map(source -> fn.apply(source.get(input)).orElse(null))
                .filter(Objects::nonNull)
                .findFirst();
    }
}
