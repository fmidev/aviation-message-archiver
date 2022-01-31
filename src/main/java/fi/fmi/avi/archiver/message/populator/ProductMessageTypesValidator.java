package fi.fmi.avi.archiver.message.populator;

import fi.fmi.avi.archiver.config.model.AviationProduct;
import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.ProcessingResult;
import fi.fmi.avi.model.MessageType;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

public class ProductMessageTypesValidator implements MessagePopulator {

    private final Map<String, Set<Integer>> productMessageTypes;

    public ProductMessageTypesValidator(final Map<MessageType, Integer> typeIds, final Map<String, AviationProduct> aviationProducts,
                                        final Map<String, Set<MessageType>> productMessageTypes) {
        requireNonNull(typeIds, "typeIds");
        requireNonNull(aviationProducts, "aviationProducts");
        requireNonNull(productMessageTypes, "productMessageTypes");
        checkArgument(!productMessageTypes.isEmpty(), "productMessageTypes cannot be empty");
        checkArgument(productMessageTypes.values().stream()
                .flatMap(Collection::stream).allMatch(typeIds::containsKey), "messageTypes must have configured type ids");
        checkArgument(productMessageTypes.keySet().stream()
                .allMatch(aviationProducts::containsKey), "aviation products must have a configured id");

        this.productMessageTypes = productMessageTypes.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> entry.getValue().stream().map(typeIds::get).collect(Collectors.toSet()))
                );
    }

    @Override
    public void populate(final InputAviationMessage inputAviationMessage, final ArchiveAviationMessage.Builder builder) {
        requireNonNull(inputAviationMessage, "inputAviationMessage");
        requireNonNull(builder, "builder");
        MessagePopulatorHelper.tryGetInt(builder, ArchiveAviationMessage.Builder::getType).ifPresent(type -> {
            final String productIdentifier = inputAviationMessage.getFileMetadata().getFileReference().getProductIdentifier();
            if (productMessageTypes.containsKey(productIdentifier)
                    && !productMessageTypes.get(productIdentifier).contains(type)) {
                builder.setProcessingResult(ProcessingResult.FORBIDDEN_MESSAGE_TYPE);
            }
        });
    }

}
