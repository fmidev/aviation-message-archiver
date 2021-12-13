package fi.fmi.avi.archiver.message.populator;

import com.google.common.base.Preconditions;
import fi.fmi.avi.archiver.config.model.AviationProduct;
import fi.fmi.avi.archiver.file.FileMetadata;
import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;

import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * Populate {@link ArchiveAviationMessage.Builder} properties from file metadata in {@link InputAviationMessage}, including product information related to
 * the file, such as route and format. Populated file-specific metadata is the file modification time.
 */
public class FileMetadataPopulator implements MessagePopulator {
    private final Map<String, AviationProduct> products;

    public FileMetadataPopulator(final Map<String, AviationProduct> products) {
        this.products = requireNonNull(products, "products");
    }

    @Override
    public void populate(final InputAviationMessage input, final ArchiveAviationMessage.Builder builder) {
        requireNonNull(input, "input");
        requireNonNull(builder, "builder");
        final FileMetadata fileMetadata = input.getFileMetadata();
        final AviationProduct product = products.get(fileMetadata.getProductIdentifier());
        // TODO Logging / exception message
        Preconditions.checkState(product != null, "Unknown product identifier: %s; unable to resolve route", fileMetadata.getProductIdentifier());

        builder.setRoute(product.getRouteId())//
                .setFormat(fileMetadata.getFileConfig().getFormatId())//
                .setFileModified(fileMetadata.getFileModified());
    }
}
