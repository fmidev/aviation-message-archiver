package fi.fmi.avi.archiver.message.populator.conditional;

import fi.fmi.avi.archiver.config.model.AviationProduct;
import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessageOrBuilder;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;

import static java.util.Objects.requireNonNull;

public class ProductIdPropertyReader extends AbstractConditionPropertyReader<String> {
    private final Set<String> productIds;

    public ProductIdPropertyReader(final Map<String, AviationProduct> aviationProducts) {
        this.productIds = requireNonNull(aviationProducts, "aviationProducts").keySet();
    }

    @Nullable
    @Override
    public String readValue(final InputAviationMessage input, final ArchiveAviationMessageOrBuilder target) {
        requireNonNull(input, "input");
        requireNonNull(target, "target");
        return input.getFileMetadata().getFileReference().getProductId();
    }

    @Override
    public boolean validate(final String value) {
        requireNonNull(value, "value");
        return productIds.contains(value);
    }
}
