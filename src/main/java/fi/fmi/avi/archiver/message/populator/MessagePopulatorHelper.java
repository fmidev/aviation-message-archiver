package fi.fmi.avi.archiver.message.populator;

import static java.util.Objects.requireNonNull;

import java.time.Clock;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Function;

import javax.annotation.Nullable;

import fi.fmi.avi.archiver.file.FileMetadata;
import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.file.InputBulletinHeading;
import fi.fmi.avi.archiver.util.TimeUtil;
import fi.fmi.avi.model.PartialDateTime;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.PartialOrCompleteTimePeriod;

/**
 * A helper class primarily used by {@link MessagePopulator} implementations.
 */
public class MessagePopulatorHelper {
    private final Clock clock;

    public MessagePopulatorHelper(final Clock clock) {
        this.clock = requireNonNull(clock, "clock");
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
    public static <F> OptionalInt tryGetInt(final F input, final Function<F, Integer> reader) {
        try {
            return OptionalInt.of(reader.apply(input));
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

    /**
     * Attempts to resolve the given {@code instantToComplete} into a complete time.
     * If {@code instantToComplete} already has {@link PartialOrCompleteTimeInstant#getCompleteTime() complete time}, it is returned. Otherwise, the
     * {@link PartialOrCompleteTimeInstant#getPartialTime() partial time} is completed nearest to the reference times declared below. Missing reference times
     * are skipped over. If a reference time is partial time, it is completed nearest to the next reference time. The reference times are used in this order:
     *
     * <ul>
     *     <li>timestamp parsed from file name</li>
     *     <li>file modification time</li>
     *     <li>current time of clock</li>
     * </ul>
     * <p>
     * The partial time is assumed to be in {@link ZoneOffset#UTC UTC} if it has no zone information.
     *
     * @param instantToComplete partial or complete time instant to complete
     * @param inputFileMetadata input file metadata
     * @return the complete time or empty if no complete time could be resolved
     */
    public Optional<ZonedDateTime> resolveCompleteTime(final PartialOrCompleteTimeInstant instantToComplete, final FileMetadata inputFileMetadata) {
        requireNonNull(instantToComplete, "instantToComplete");
        requireNonNull(inputFileMetadata, "inputFileMetadata");
        if (instantToComplete.getCompleteTime().isPresent()) {
            return instantToComplete.getCompleteTime();
        }
        @Nullable final PartialDateTime zonedPartial = instantToComplete.getPartialTime()//
                .map(partial -> partial.withZone(partial.getZone().orElse(ZoneOffset.UTC)))//
                .orElse(null);
        if (zonedPartial == null) {
            return Optional.empty();
        }
        return TimeUtil.toCompleteTime(zonedPartial, referenceTimeCandidates(inputFileMetadata));
    }

    /**
     * Attempts to complete the provided {@code periodToComplete}.
     * If {@code periodToComplete} already has complete start and end time, it is returned as is. Otherwise, if it has either a complete start or end time, it
     * is used as a reference for completion.
     * <p>
     * If {@code periodToComplete} already has a complete start and end time, it is returned as is. Otherwise, the
     * partial period is completed nearest to the reference times declared below. Missing reference times are skipped over. If the reference time is a partial
     * time, it is completed nearest to the next reference time. The reference times are used in this order:
     *
     * <ul>
     *     <li>complete start time (ensuring end time is completed after start time)</li>
     *     <li>complete end time (ensuring start time is completed before end time)</li>
     *     <li>provided {@code primaryReferenceTime}</li>
     *     <li>timestamp parsed from file name</li>
     *     <li>file modification time</li>
     *     <li>current time of clock</li>
     * </ul>
     * <p>
     * The partial time is assumed to be in {@link ZoneOffset#UTC UTC} if it has no zone information.
     *
     * @param periodToComplete     partial or complete time period to complete
     * @param primaryReferenceTime primary reference time
     * @param inputFileMetadata    in put file metadata
     * @return result of completion, which contains complete times only if the completion was successful
     */
    public PartialOrCompleteTimePeriod tryCompletePeriod(final PartialOrCompleteTimePeriod periodToComplete,
                                                         final @Nullable PartialOrCompleteTimeInstant primaryReferenceTime, final FileMetadata inputFileMetadata) {
        requireNonNull(periodToComplete, "periodToComplete");
        requireNonNull(inputFileMetadata, "inputFileMetadata");
        @Nullable final ZonedDateTime completeStartTime = periodToComplete.getStartTime().flatMap(PartialOrCompleteTimeInstant::getCompleteTime).orElse(null);
        @Nullable final ZonedDateTime completeEndTime = periodToComplete.getEndTime().flatMap(PartialOrCompleteTimeInstant::getCompleteTime).orElse(null);
        try {
            if (completeStartTime != null && completeEndTime != null) {
                return periodToComplete;
            } else if (completeStartTime != null) {
                return periodToComplete.toBuilder()//
                        .mapEndTime(time -> time.toBuilder()//
                                .completePartial(partial -> partial.toZonedDateTimeAfter(completeStartTime))//
                                .build())//
                        .build();
            } else if (completeEndTime != null) {
                return periodToComplete.toBuilder()//
                        .mapStartTime(time -> time.toBuilder()//
                                .completePartial(partial -> partial.toZonedDateTimeBefore(completeEndTime))//
                                .build())//
                        .build();
            } else {
                @Nullable final ZonedDateTime referenceTime = TimeUtil.toCompleteTime(referenceTimeCandidates(primaryReferenceTime, inputFileMetadata)).orElse(null);
                if (referenceTime == null) {
                    return periodToComplete;
                }
                return periodToComplete.toBuilder()//
                        .completePartialStartingNear(referenceTime)//
                        .build();
            }
        } catch (final RuntimeException ignored) {
            return periodToComplete;
        }
    }

    private List<PartialOrCompleteTimeInstant> referenceTimeCandidates(final FileMetadata inputFileMetadata) {
        return referenceTimeCandidates(null, inputFileMetadata);
    }

    private List<PartialOrCompleteTimeInstant> referenceTimeCandidates(@Nullable final PartialOrCompleteTimeInstant primaryInstant,
                                                                       final FileMetadata inputFileMetadata) {
        final List<PartialOrCompleteTimeInstant> builder = new ArrayList<>();
        builder.add(primaryInstant);
        builder.add(inputFileMetadata.createFilenameMatcher().getTimestamp(clock).orElse(null));
        builder.add(inputFileMetadata.getFileModified()//
                .map(fileModified -> PartialOrCompleteTimeInstant.of(fileModified.atZone(ZoneOffset.UTC)))//
                .orElse(null));
        builder.add(PartialOrCompleteTimeInstant.of(ZonedDateTime.now(clock)));
        return Collections.unmodifiableList(builder);
    }

}
