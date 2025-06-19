package fi.fmi.avi.archiver.message.processor.conditional;

import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessageOrBuilder;
import fi.fmi.avi.archiver.message.processor.populator.BulletinHeadingSource;
import fi.fmi.avi.model.bulletin.BulletinHeading;

import javax.annotation.Nullable;
import java.util.List;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

public class DataDesignatorPropertyReader extends AbstractBulletinHeadingConditionPropertyReader<String> {
    private static final Pattern DESIGNATOR_PATTERN = Pattern.compile("^[A-Z]{4}[0-9]{2}$");
    private final List<BulletinHeadingSource> bulletinHeadingSources;

    public DataDesignatorPropertyReader(final List<BulletinHeadingSource> bulletinHeadingSources) {
        this.bulletinHeadingSources = requireNonNull(bulletinHeadingSources, "bulletinHeadingSources");
        checkArgument(!bulletinHeadingSources.isEmpty(), "bulletinHeadingSources must not be empty");
    }

    @Override
    protected List<BulletinHeadingSource> getBulletinHeadingSources() {
        return bulletinHeadingSources;
    }

    @Nullable
    @Override
    public String readValue(final InputAviationMessage input, final ArchiveAviationMessageOrBuilder target) {
        requireNonNull(input, "input");
        requireNonNull(target, "target");
        return getFirstNonNullFromBulletinHeading(input, heading -> heading.getBulletinHeading()//
                .map(BulletinHeading::getDataDesignatorsForTAC))//
                .orElse(null);
    }

    @Override
    public boolean validate(final String value) {
        requireNonNull(value, "value");
        return DESIGNATOR_PATTERN.matcher(value).matches();
    }
}
