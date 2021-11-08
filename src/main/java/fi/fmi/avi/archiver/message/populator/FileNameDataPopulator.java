package fi.fmi.avi.archiver.message.populator;

import static java.util.Objects.requireNonNull;

import fi.fmi.avi.archiver.file.FilenameMatcher;
import fi.fmi.avi.archiver.file.InputAviationMessage;
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

    public FileNameDataPopulator(final MessagePopulatorHelper helper) {
        this.helper = requireNonNull(helper, "helper");
    }

    @Override
    public void populate(final InputAviationMessage input, final ArchiveAviationMessage.Builder builder) {
        requireNonNull(input, "input");
        requireNonNull(builder, "builder");
        final FilenameMatcher filenameMatcher = input.getFileMetadata().createFilenameMatcher();

        filenameMatcher.getTimestamp()//
                .flatMap(timestamp -> helper.resolveCompleteTime(timestamp, input.getFileMetadata()))
                .ifPresent(timestamp -> builder.setMessageTime(timestamp.toInstant()));
    }
}
