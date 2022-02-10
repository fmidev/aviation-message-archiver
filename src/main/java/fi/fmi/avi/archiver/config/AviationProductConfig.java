package fi.fmi.avi.archiver.config;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.context.annotation.Bean;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

import fi.fmi.avi.archiver.config.model.AviationProduct;
import fi.fmi.avi.archiver.config.model.FileConfig;
import fi.fmi.avi.model.GenericAviationWeatherMessage;

@ConstructorBinding
@ConfigurationProperties(prefix = "production-line-initialization")
public class AviationProductConfig {

    private final List<AviationProduct.Builder> productBuilders;

    AviationProductConfig(final List<AviationProduct.Builder> products) {
        this.productBuilders = requireNonNull(products, "products");
    }

    private static Map<String, AviationProduct> buildProducts(final List<AviationProduct.Builder> productBuilders, final Map<String, Integer> messageRouteIds,
            final Map<GenericAviationWeatherMessage.Format, Integer> messageFormatIds) {
        validateProducts(productBuilders);
        final ImmutableMap.Builder<String, AviationProduct> builder = ImmutableMap.builder();
        iterateProducts(productBuilders, productBuilder -> {
            mapRouteToId(productBuilder, messageRouteIds);
            mapFormatsToIds(productBuilder, messageFormatIds);
            final AviationProduct product = productBuilder.build();
            builder.put(product.getId(), product);
        });
        return builder.build();
    }

    private static void validateProducts(final List<AviationProduct.Builder> productBuilders) {
        checkState(!productBuilders.isEmpty(), "Products are missing");
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
                    "Invalid configuration: archive directory of product(s) <%s> is equal to fail directory of <%s>: <%s>; "
                            + "this is allowed only when both archive and fail directories are the same", archiveDirProducts,
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

    private static <E> void iterateProducts(final List<E> productBuilders, final Consumer<? super E> productConsumer) {
        requireNonNull(productBuilders, "productBuilders");
        requireNonNull(productConsumer, "productConsumer");

        final int size = productBuilders.size();
        for (int i = 0; i < size; i++) {
            try {
                productConsumer.accept(productBuilders.get(i));
            } catch (final RuntimeException e) {
                throw new IllegalStateException("Product at index <" + i + "> is invalid: " + e.getMessage(), e);
            }
        }
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
        return buildProducts(productBuilders, messageRouteIds, messageFormatIds);
    }

}
