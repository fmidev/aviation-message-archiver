package fi.fmi.avi.archiver.message.populator;

import static java.util.Objects.requireNonNull;

import java.util.Map;

import com.google.common.base.Preconditions;

import fi.fmi.avi.archiver.config.model.AviationProduct;
import fi.fmi.avi.archiver.file.FileMetadata;
import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;

/**
 * Populate {@link ArchiveAviationMessage.Builder} properties from {@link FileMetadata file metadata} in {@link InputAviationMessage}, including product
 * information related to the file, such as route and format. Populated file-specific metadata is the file modification time.
 */
public class FileMetadataPopulator implements MessagePopulator {
    private final Map<String, AviationProduct> products;

    public FileMetadataPopulator(final Map<String, AviationProduct> products) {
        this.products = requireNonNull(products, "products");
    }

    @Override
    public void populate(final MessagePopulatingContext context, final ArchiveAviationMessage.Builder target) {
        requireNonNull(context, "context");
        requireNonNull(target, "target");
        final FileMetadata fileMetadata = context.getInputMessage().getFileMetadata();
        final AviationProduct product = products.get(fileMetadata.getFileReference().getProductId());
        Preconditions.checkState(product != null, "Unknown product identifier: %s; unable to resolve route", fileMetadata.getFileReference().getProductId());

        target.setRoute(product.getRouteId())//
                .setFormat(fileMetadata.getFileConfig().getFormatId())//
                .setFileModified(fileMetadata.getFileModified());
    }
}
