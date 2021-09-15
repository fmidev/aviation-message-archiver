package fi.fmi.avi.archiver.message.populator;

import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.ProcessingResult;

import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

public class MessageTypeValidator implements MessagePopulator {

    private String productIdentifier;
    private Set<Integer> typeIdentifiers;

    public void setProductIdentifier(final String productIdentifier) {
        requireNonNull(productIdentifier, "productIdentifier");
        checkArgument(!productIdentifier.isEmpty(), "productIdentifier cannot be empty");
        this.productIdentifier = productIdentifier;
    }

    public void setTypeIdentifiers(final Set<Integer> typeIdentifiers) {
        requireNonNull(typeIdentifiers, "typeIdentifiers");
        checkArgument(!typeIdentifiers.isEmpty(), "typeIdentifiers cannot be empty");
        this.typeIdentifiers = typeIdentifiers;
    }

    @Override
    public void populate(final InputAviationMessage inputAviationMessage, final ArchiveAviationMessage.Builder builder) {
        requireNonNull(inputAviationMessage, "inputAviationMessage");
        requireNonNull(builder, "builder");
        if (inputAviationMessage.getFileMetadata().getProductIdentifier().equals(productIdentifier)
                && !typeIdentifiers.contains(builder.getType())) {
            builder.setProcessingResult(ProcessingResult.INVALID_MESSAGE_TYPE);
        }
    }

}
