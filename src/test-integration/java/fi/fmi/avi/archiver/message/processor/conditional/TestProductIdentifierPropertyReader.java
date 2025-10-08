package fi.fmi.avi.archiver.message.processor.conditional;

import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessageOrBuilder;

import java.util.Set;

import static java.util.Objects.requireNonNull;

public class TestProductIdentifierPropertyReader extends AbstractConditionPropertyReader<String> {
    private final Set<String> aviationProductIds;

    public TestProductIdentifierPropertyReader(final Set<String> aviationProductIds) {
        this.aviationProductIds = requireNonNull(aviationProductIds, "aviationProductIds");
    }

    @Override
    public String readValue(final InputAviationMessage input, final ArchiveAviationMessageOrBuilder target) {
        requireNonNull(input, "input");
        requireNonNull(target, "target");
        return input.getFileMetadata().getFileReference().getProductId();
    }

    @Override
    public boolean validate(final String value) {
        requireNonNull(value, "value");
        return aviationProductIds.contains(value);
    }
}
