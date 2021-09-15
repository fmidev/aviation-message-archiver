package fi.fmi.avi.archiver.util;

import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.file.InputBulletinHeading;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public class BulletinUtil {

    private BulletinUtil() {
        throw new AssertionError();
    }

    public static <T> Optional<T> getFirstNonNullFromBulletinHeading(final Collection<BulletinHeadingSource> bulletinHeadingSources,
                                                                     final InputAviationMessage input, final Function<InputBulletinHeading, Optional<T>> fn) {
        return bulletinHeadingSources.stream()
                .map(source -> fn.apply(source.get(input)).orElse(null))
                .filter(Objects::nonNull)
                .findFirst();
    }

}
