package fi.fmi.avi.archiver.message.populator;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import fi.fmi.avi.archiver.config.model.AviationProduct;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.ProcessingResult;
import fi.fmi.avi.model.MessageType;

public class ProductMessageTypesValidator implements MessagePopulator {

    private final Map<String, Set<Integer>> productMessageTypes;

    public ProductMessageTypesValidator(final Map<MessageType, Integer> typeIds, final Map<String, AviationProduct> aviationProducts,
            final Map<String, Set<MessageType>> productMessageTypes) {
        requireNonNull(typeIds, "typeIds");
        requireNonNull(aviationProducts, "aviationProducts");
        requireNonNull(productMessageTypes, "productMessageTypes");
        checkArgument(!productMessageTypes.isEmpty(), "productMessageTypes cannot be empty");
        checkArgument(productMessageTypes.values().stream().flatMap(Collection::stream).allMatch(typeIds::containsKey),
                "messageTypes must have configured type ids");
        checkArgument(productMessageTypes.keySet().stream().allMatch(aviationProducts::containsKey), "aviation products must have a configured id");

        this.productMessageTypes = productMessageTypes.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().stream().map(typeIds::get).collect(Collectors.toSet())));
    }

    @Override
    public void populate(final MessagePopulatingContext context, final ArchiveAviationMessage.Builder target) {
        requireNonNull(context, "context");
        requireNonNull(target, "target");
        MessagePopulatorHelper.tryGetInt(target, ArchiveAviationMessage.Builder::getType).ifPresent(type -> {
            final String productId = context.getInputMessage().getFileMetadata().getFileReference().getProductId();
            if (productMessageTypes.containsKey(productId) && !productMessageTypes.get(productId).contains(type)) {
                target.setProcessingResult(ProcessingResult.FORBIDDEN_MESSAGE_TYPE);
            }
        });
    }

}
