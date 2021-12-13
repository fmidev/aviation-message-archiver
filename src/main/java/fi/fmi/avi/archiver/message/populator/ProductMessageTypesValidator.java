package fi.fmi.avi.archiver.message.populator;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.ProcessingResult;
import fi.fmi.avi.model.MessageType;

public class ProductMessageTypesValidator implements MessagePopulator {

    private final String productIdentifier;
    private final Set<Integer> typeIdentifiers;

    public ProductMessageTypesValidator(final Map<MessageType, Integer> typeIds, final String productIdentifier, final Set<MessageType> messageTypes) {
        requireNonNull(typeIds, "typeIds");
        requireNonNull(productIdentifier, "productIdentifier");
        checkArgument(!productIdentifier.isEmpty(), "productIdentifier cannot be empty");
        this.productIdentifier = productIdentifier;

        requireNonNull(messageTypes, "messageTypes");
        checkArgument(!messageTypes.isEmpty(), "messageTypes cannot be empty");
        checkArgument(messageTypes.stream().allMatch(typeIds::containsKey), "messageTypes must have configured type ids");
        this.typeIdentifiers = messageTypes.stream().map(typeIds::get).collect(Collectors.toSet());
    }

    @Override
    public void populate(final InputAviationMessage inputAviationMessage, final ArchiveAviationMessage.Builder builder) {
        requireNonNull(inputAviationMessage, "inputAviationMessage");
        requireNonNull(builder, "builder");
        MessagePopulatorHelper.tryGetInt(builder, ArchiveAviationMessage.Builder::getType).ifPresent(type -> {
            if (inputAviationMessage.getFileMetadata().getFileReference().getProductIdentifier().equals(productIdentifier) && !typeIdentifiers.contains(type)) {
                builder.setProcessingResult(ProcessingResult.FORBIDDEN_MESSAGE_TYPE);
            }
        });
    }

}
