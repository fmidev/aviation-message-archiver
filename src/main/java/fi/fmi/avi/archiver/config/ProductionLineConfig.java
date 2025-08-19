package fi.fmi.avi.archiver.config;

import com.google.common.collect.*;
import fi.fmi.avi.archiver.config.model.AviationProduct;
import fi.fmi.avi.archiver.config.model.FileConfig;
import fi.fmi.avi.archiver.config.model.MessagePopulatorInstanceSpec;
import fi.fmi.avi.archiver.config.model.PostActionInstanceSpec;
import fi.fmi.avi.model.GenericAviationWeatherMessage;
import fi.fmi.avi.model.MessageType;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.context.annotation.Bean;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

@ConfigurationProperties(prefix = "production-line")
public class ProductionLineConfig {
    private final List<AviationProduct.Builder> aviationProductBuilders;
    private final List<MessagePopulatorInstanceSpec.Builder> messagePopulatorSpecBuilders;
    private final List<PostActionInstanceSpec.Builder> postActionInstanceSpecBuilders;
    private final Map<String, Integer> routeIds;
    private final Map<GenericAviationWeatherMessage.Format, Integer> formatIds;
    private final Map<MessageType, Integer> typeIds;

    @ConstructorBinding
    ProductionLineConfig(final List<AviationProduct.Builder> products,
                         final List<MessagePopulatorInstanceSpec.Builder> messagePopulators,
                         @Nullable final List<PostActionInstanceSpec.Builder> postActions,
                         final Map<String, Integer> routeIds,
                         final Map<GenericAviationWeatherMessage.Format, Integer> formatIds,
                         final Map<MessageType, Integer> typeIds) {
        this.aviationProductBuilders = requireNonNull(products, "products");
        this.messagePopulatorSpecBuilders = requireNonNull(messagePopulators, "messagePopulators");
        this.postActionInstanceSpecBuilders = postActions == null ? List.of() : postActions;
        this.routeIds = requireNonNull(routeIds, "routeIds");
        this.formatIds = requireNonNull(formatIds, "formatIds");
        this.typeIds = requireNonNull(typeIds, "typeIds");
    }

    private static <E> void iterate(final String description, final List<E> elements, final Consumer<? super E> elementConsumer) {
        int i = 0;
        for (final E element : elements) {
            try {
                elementConsumer.accept(element);
            } catch (final RuntimeException e) {
                throw new IllegalStateException("Invalid " + description + " at index <" + i + ">: " + e.getMessage(), e);
            }
            i++;
        }
    }

    private static void validateAviationProducts(final List<AviationProduct.Builder> productBuilders) {
        checkState(!productBuilders.isEmpty(), "Invalid configuration: products is empty");
        validateInputDirs(productBuilders);
        validateInputDirPatterns(productBuilders);
    }

    private static void validateInputDirs(final List<AviationProduct.Builder> productBuilders) {
        final SetMultimap<Path, String> productInputDirs = HashMultimap.create();
        final SetMultimap<Path, String> productArchiveDirs = HashMultimap.create();
        final SetMultimap<Path, String> productFailDirs = HashMultimap.create();
        iterateProducts(productBuilders, builder -> {
            productInputDirs.put(builder.getInputDir(), builder.getId());
            productArchiveDirs.put(builder.getArchiveDir(), builder.getId());
            productFailDirs.put(builder.getFailDir(), builder.getId());
        });

        final String errorMessageTemplate = "Invalid configuration: product(s) <%s> have %s directory equal to %s directory of <%s>: <%s>";
        productArchiveDirs.asMap().forEach((archiveDir, productIds) -> {
            final Set<String> conflictingProducts = productInputDirs.get(archiveDir);
            checkState(conflictingProducts.isEmpty(), errorMessageTemplate, productIds, "archive", "input", conflictingProducts, archiveDir);
        });
        productFailDirs.asMap().forEach((failDir, productIds) -> {
            final Set<String> conflictingProducts = productInputDirs.get(failDir);
            checkState(conflictingProducts.isEmpty(), errorMessageTemplate, productIds, "fail", "input", conflictingProducts, failDir);
        });

        Sets.intersection(productArchiveDirs.keySet(), productFailDirs.keySet()).forEach(commonPath -> {
            final Set<String> archiveDirProducts = productArchiveDirs.get(commonPath);
            final Set<String> failDirProducts = productFailDirs.get(commonPath);
            checkState(archiveDirProducts.equals(failDirProducts),
                    "Invalid configuration: archive directory of product(s) <%s> is equal to fail directory of <%s>: <%s>; " + "this is allowed only when both archive and fail directories are the same", archiveDirProducts,
                    Sets.difference(failDirProducts, archiveDirProducts), commonPath);
        });
    }

    private static void validateInputDirPatterns(final List<AviationProduct.Builder> productBuilders) {
        final SetMultimap<Path, String> inputPatterns = HashMultimap.create();
        iterateProducts(productBuilders, builder -> builder.getFiles().forEach(fileConfig -> {
            final Path inputDir = builder.getInputDir();
            final String pattern = fileConfig.getPattern().pattern();
            checkState(!inputPatterns.containsEntry(inputDir, pattern), "Duplicate pattern <%s> for input dir <%s>", pattern, inputDir);
            inputPatterns.put(inputDir, pattern);
        }));
    }

    private static void iterateProducts(final List<AviationProduct.Builder> productBuilders, final Consumer<? super AviationProduct.Builder> productConsumer) {
        iterate("product configuration", productBuilders, productConsumer);
    }

    private static void mapRouteToId(final AviationProduct.Builder product, final Map<String, Integer> messageRouteIds) {
        final Integer routeId = messageRouteIds.get(product.getRoute());
        checkState(routeId != null, "Unknown route <%s> for product <%s>", product.getRoute(), product.getId());
        product.setRouteId(routeId);
    }

    private static void mapFormatsToIds(final AviationProduct.Builder product, final Map<GenericAviationWeatherMessage.Format, Integer> messageFormatIds) {
        for (final FileConfig.Builder file : product.getFiles()) {
            final Integer formatId = messageFormatIds.get(file.getFormat());
            checkState(formatId != null, "Unknown file message format <%s> for product <%s>", file.getFormat(), product.getId());
            file.setFormatId(formatId);
        }
    }

    @Bean
    Map<String, AviationProduct> aviationProducts(final Map<String, Integer> messageRouteIds,
                                                  final Map<GenericAviationWeatherMessage.Format, Integer> messageFormatIds) {
        validateAviationProducts(aviationProductBuilders);
        final ImmutableMap.Builder<String, AviationProduct> builder = ImmutableMap.builder();
        iterateProducts(aviationProductBuilders, productBuilder -> {
            mapRouteToId(productBuilder, messageRouteIds);
            mapFormatsToIds(productBuilder, messageFormatIds);
            final AviationProduct product = productBuilder.build();
            builder.put(product.getId(), product);
        });
        return builder.build();
    }

    @Bean
    List<MessagePopulatorInstanceSpec> messagePopulatorSpecs() {
        checkState(!messagePopulatorSpecBuilders.isEmpty(), "Invalid message populators configuration: messagePopulators is empty");
        final ImmutableList.Builder<MessagePopulatorInstanceSpec> specs = ImmutableList.builder();
        iterate("MessagePopulator specification", messagePopulatorSpecBuilders, builder -> specs.add(builder.build()));
        return specs.build();
    }

    @Bean
    List<PostActionInstanceSpec> postActionSpecs() {
        final ImmutableList.Builder<PostActionInstanceSpec> specs = ImmutableList.builder();
        iterate("PostAction specification", postActionInstanceSpecBuilders, builder -> specs.add(builder.build()));
        return specs.build();
    }

    @Bean
    BiMap<String, Integer> messageRouteIds() {
        checkState(!routeIds.isEmpty(), "Invalid configuration: routeIds is empty");
        return ImmutableBiMap.copyOf(routeIds);
    }

    @Bean
    BiMap<GenericAviationWeatherMessage.Format, Integer> messageFormatIds() {
        checkState(!formatIds.isEmpty(), "Invalid configuration: formatIds is empty");
        return ImmutableBiMap.copyOf(formatIds);
    }

    @Bean
    BiMap<MessageType, Integer> messageTypeIds() {
        checkState(!typeIds.isEmpty(), "Invalid configuration: typeIds is empty");
        return ImmutableBiMap.copyOf(typeIds);
    }
}
