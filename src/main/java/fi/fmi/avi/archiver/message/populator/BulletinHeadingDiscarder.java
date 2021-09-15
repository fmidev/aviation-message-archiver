package fi.fmi.avi.archiver.message.populator;

import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.file.InputBulletinHeading;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.MessageDiscardedException;
import fi.fmi.avi.archiver.util.BulletinHeadingSource;
import fi.fmi.avi.archiver.util.BulletinUtil;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

/**
 * Discard a message if the given pattern is found in its bulletin heading string.
 */
public class BulletinHeadingDiscarder implements MessagePopulator {

    private List<BulletinHeadingSource> bulletinHeadingSources;
    private Pattern headingPattern;

    public void setBulletinHeadingSources(final List<BulletinHeadingSource> bulletinHeadingSources) {
        requireNonNull(bulletinHeadingSources, "bulletinHeadingSources");
        checkArgument(!bulletinHeadingSources.isEmpty(), "bulletinHeadingSources cannot be empty");
        this.bulletinHeadingSources = bulletinHeadingSources;
    }

    public void setHeadingPattern(final Pattern headingPattern) {
        this.headingPattern = requireNonNull(headingPattern, "headingPattern");
    }

    @Override
    public void populate(final InputAviationMessage inputAviationMessage, @Nullable final ArchiveAviationMessage.Builder builder)
            throws MessageDiscardedException {
        requireNonNull(inputAviationMessage, "inputAviationMessage");
        final Optional<String> heading = BulletinUtil.getFirstNonNullFromBulletinHeading(bulletinHeadingSources, inputAviationMessage,
                InputBulletinHeading::getBulletinHeadingString);
        if (heading.isPresent() && headingPattern.matcher(heading.get()).find()) {
            throw new MessageDiscardedException("Discarded message with heading: " + heading.get());
        }
    }

}
