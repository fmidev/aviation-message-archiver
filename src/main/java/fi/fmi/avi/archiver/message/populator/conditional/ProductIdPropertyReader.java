package fi.fmi.avi.archiver.message.populator.conditional;

import static java.util.Objects.requireNonNull;

import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import fi.fmi.avi.archiver.config.model.AviationProduct;
import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;

public class ProductIdPropertyReader extends AbstractConditionPropertyReader<String> {
    private final Set<String> productIdentifiers;

    public ProductIdPropertyReader(final Map<String, AviationProduct> aviationProducts) {
        this.productIdentifiers = requireNonNull(aviationProducts, "aviationProducts").keySet();
    }

    @Nullable
    @Override
    public String readValue(final InputAviationMessage input, final ArchiveAviationMessage.Builder target) {
        requireNonNull(input, "input");
        requireNonNull(target, "target");
        return input.getFileMetadata().getFileReference().getProductIdentifier();
    }

    @Override
    public boolean validate(final String value) {
        requireNonNull(value, "value");
        return productIdentifiers.contains(value);
    }
}
