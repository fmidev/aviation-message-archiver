package fi.fmi.avi.archiver.message.populator;

import static java.util.Objects.requireNonNull;

import java.time.Clock;

import fi.fmi.avi.archiver.file.FilenameMatcher;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;

/**
 * Populate {@link ArchiveAviationMessage.Builder} properties from values parsed from file name.
 *
 * <p>
 * Currently supports only populating {@link ArchiveAviationMessage#getMessageTime()} from timestamp.
 * </p>
 */
public class FileNameDataPopulator implements MessagePopulator {
    private final MessagePopulatorHelper helper;
    private final Clock clock;

    public FileNameDataPopulator(final MessagePopulatorHelper helper, final Clock clock) {
        this.helper = requireNonNull(helper, "helper");
        this.clock = requireNonNull(clock, "clock");
    }

    @Override
    public void populate(final MessagePopulatingContext context, final ArchiveAviationMessage.Builder target) {
        requireNonNull(context, "context");
        requireNonNull(target, "target");
        final FilenameMatcher filenameMatcher = context.getInputMessage().getFileMetadata().createFilenameMatcher();

        filenameMatcher.getTimestamp(clock)//
                .flatMap(timestamp -> helper.resolveCompleteTime(timestamp, context.getInputMessage().getFileMetadata()))
                .ifPresent(timestamp -> target.setMessageTime(timestamp.toInstant()));
    }
}
