package fi.fmi.avi.archiver.message.populator;

import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.file.InputBulletinHeading;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.MessageDiscardedException;
import fi.fmi.avi.model.bulletin.BulletinHeading;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;
import static fi.fmi.avi.archiver.message.populator.MessagePopulatorHelper.DEFAULT_BULLETIN_HEADING_SOURCES;
import static java.util.Objects.requireNonNull;

/**
 * Discard a message if the given pattern is found in its bulletin heading data designator.
 */
public class DataDesignatorDiscarder implements MessagePopulator {

    private final Pattern pattern;
    private List<BulletinHeadingSource> bulletinHeadingSources = DEFAULT_BULLETIN_HEADING_SOURCES;

    public DataDesignatorDiscarder(final Pattern pattern) {
        this.pattern = requireNonNull(pattern, "pattern");
    }

    public void setBulletinHeadingSources(final List<BulletinHeadingSource> bulletinHeadingSources) {
        requireNonNull(bulletinHeadingSources, "bulletinHeadingSources");
        checkArgument(!bulletinHeadingSources.isEmpty(), "bulletinHeadingSources cannot be empty");
        this.bulletinHeadingSources = bulletinHeadingSources;
    }

    @Override
    public void populate(final InputAviationMessage inputAviationMessage, @Nullable final ArchiveAviationMessage.Builder ignored)
            throws MessageDiscardedException {
        requireNonNull(inputAviationMessage, "inputAviationMessage");
        final Optional<String> dataDesignators = MessagePopulatorHelper.getFirstNonNullFromBulletinHeading(bulletinHeadingSources, inputAviationMessage,
                InputBulletinHeading::getBulletinHeading).map(BulletinHeading::getDataDesignatorsForTAC);
        if (dataDesignators.isPresent() && pattern.matcher(dataDesignators.get()).matches()) {
            throw new MessageDiscardedException("Discarded message with dataDesignators: " + dataDesignators.get());
        }
    }


}
