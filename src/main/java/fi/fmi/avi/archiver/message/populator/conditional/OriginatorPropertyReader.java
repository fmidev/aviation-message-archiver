package fi.fmi.avi.archiver.message.populator.conditional;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.populator.BulletinHeadingSource;
import fi.fmi.avi.model.bulletin.BulletinHeading;

public class OriginatorPropertyReader extends AbstractBulletinHeadingConditionPropertyReader<String> {
    private static final Pattern ORIGINATOR_PATTERN = Pattern.compile("^[A-Z]{4}$");
    private final List<BulletinHeadingSource> bulletinHeadingSources;

    public OriginatorPropertyReader(final List<BulletinHeadingSource> bulletinHeadingSources) {
        this.bulletinHeadingSources = requireNonNull(bulletinHeadingSources, "bulletinHeadingSources");
        checkArgument(!bulletinHeadingSources.isEmpty(), "bulletinHeadingSources must not be empty");
    }

    @Override
    protected List<BulletinHeadingSource> getBulletinHeadingSources() {
        return bulletinHeadingSources;
    }

    @Nullable
    @Override
    public String readValue(final InputAviationMessage input, final ArchiveAviationMessage.Builder target) {
        requireNonNull(input, "input");
        requireNonNull(target, "target");
        return getFirstNonNullFromBulletinHeading(input, heading -> heading.getBulletinHeading()//
                .map(BulletinHeading::getLocationIndicator))//
                .orElse(null);
    }

    @Override
    public boolean validate(final String value) {
        requireNonNull(value, "value");
        return ORIGINATOR_PATTERN.matcher(value).matches();
    }
}
