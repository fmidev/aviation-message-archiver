package fi.fmi.avi.archiver.message.processor.populator;

import fi.fmi.avi.archiver.file.FilenameMatcher;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.processor.MessageProcessorContext;

import java.time.Clock;

import static java.util.Objects.requireNonNull;

/**
 * Populate {@link ArchiveAviationMessage.Builder} properties from values parsed from file name.
 *
 * <p>
 * Currently populates only {@link ArchiveAviationMessage#getMessageTime() message time} from timestamp.
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
    public void populate(final MessageProcessorContext context, final ArchiveAviationMessage.Builder target) {
        requireNonNull(context, "context");
        requireNonNull(target, "target");
        final FilenameMatcher filenameMatcher = context.getInputMessage().getFileMetadata().createFilenameMatcher();

        filenameMatcher.getTimestamp(clock)//
                .flatMap(timestamp -> helper.resolveCompleteTime(timestamp, context.getInputMessage().getFileMetadata()))
                .ifPresent(timestamp -> target.setMessageTime(timestamp.toInstant()));
    }
}
