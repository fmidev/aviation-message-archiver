package fi.fmi.avi.archiver.message.populator;

import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.ProcessingResult;

import java.util.Set;

import static java.util.Objects.requireNonNull;

public class MessageTypeValidator implements MessagePopulator {

    private String productIdentifier;
    private Set<Integer> typeIdentifiers;

    public void setProductIdentifier(final String productIdentifier) {
        this.productIdentifier = requireNonNull(productIdentifier, "productIdentifier");
    }

    public void setTypeIdentifiers(final Set<Integer> typeIdentifiers) {
        this.typeIdentifiers = requireNonNull(typeIdentifiers, "typeIdentifiers");
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
