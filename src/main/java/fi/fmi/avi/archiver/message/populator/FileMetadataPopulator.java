package fi.fmi.avi.archiver.message.populator;

import static java.util.Objects.requireNonNull;

import fi.fmi.avi.archiver.file.FileMetadata;
import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;

/**
 * Populate {@link ArchiveAviationMessage.Builder} properties from file metadata in {@link InputAviationMessage}, including product information related to
 * file, such as route and format. Populated file-specific metadata is file modification time.
 */
public class FileMetadataPopulator implements MessagePopulator {
    @Override
    public void populate(final InputAviationMessage input, final ArchiveAviationMessage.Builder builder) {
        requireNonNull(input, "input");
        requireNonNull(builder, "builder");
        final FileMetadata fileMetadata = input.getFileMetadata();
        builder.setRoute(1); // TODO: depends on #30
        builder.setFormat(1);  // TODO: depends on #30
        builder.setFileModified(fileMetadata.getFileModified());
    }
}
