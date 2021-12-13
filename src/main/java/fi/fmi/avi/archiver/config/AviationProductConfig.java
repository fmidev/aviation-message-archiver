package fi.fmi.avi.archiver.config;

import com.google.common.collect.ImmutableMap;
import fi.fmi.avi.archiver.config.model.AviationProduct;
import fi.fmi.avi.archiver.file.FileConfig;
import fi.fmi.avi.model.GenericAviationWeatherMessage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.context.annotation.Bean;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

@ConstructorBinding
@ConfigurationProperties(prefix = "production-line-initialization")
public class AviationProductConfig {

    private final List<AviationProduct.Builder> products;

    public AviationProductConfig(final List<AviationProduct.Builder> products) {
        this.products = requireNonNull(products, "products");
    }

    @Bean
    public Map<String, AviationProduct> aviationProducts(@Qualifier("messageRouteIds") final Map<String, Integer> messageRouteIds,
                                                         @Qualifier("messageFormatIds") final Map<GenericAviationWeatherMessage.Format, Integer> messageFormatIds) {
        mapRoutesToIds(products, messageRouteIds);
        mapFormatsToIds(products, messageFormatIds);
        return buildProducts(products);
    }

    public static <E> void iterateProducts(final List<E> productBuilders, final Consumer<? super E> productConsumer) {
        requireNonNull(productBuilders, "productBuilders");
        requireNonNull(productConsumer, "productConsumer");

        final int size = productBuilders.size();
        for (int i = 0; i < size; i++) {
            try {
                productConsumer.accept(productBuilders.get(i));
            } catch (final RuntimeException e) {
                throw new IllegalStateException("Product at index <" + i + "> is invalid", e);
            }
        }
    }

    private static Map<String, AviationProduct> buildProducts(final List<AviationProduct.Builder> productBuilders) {
        checkState(!productBuilders.isEmpty(), "Products are missing");
        final ImmutableMap.Builder<String, AviationProduct> builder = ImmutableMap.builder();
        iterateProducts(productBuilders, productBuilder -> {
            final AviationProduct product = productBuilder.build();
            builder.put(product.getId(), product);
        });
        return builder.build();
    }

    private static void mapRoutesToIds(final List<AviationProduct.Builder> products, final Map<String, Integer> messageRouteIds) {
        iterateProducts(products, product -> {
            final Integer routeId = messageRouteIds.get(product.getRoute());
            if (routeId == null) {
                throw new IllegalStateException(String.format(Locale.ROOT, "Unknown route <%s> for product <%s>", product.getRoute(), product.getId()));
            }
            product.setRouteId(routeId);
        });
    }

    private static void mapFormatsToIds(final List<AviationProduct.Builder> products,
                                        final Map<GenericAviationWeatherMessage.Format, Integer> messageFormatIds) {
        iterateProducts(products, product -> {
            for (final FileConfig.Builder file : product.getFiles()) {
                final Integer formatId = messageFormatIds.get(file.getFormat());
                if (formatId == null) {
                    throw new IllegalStateException(
                            String.format(Locale.ROOT, "Unknown file message format <%s> for product <%s>", file.getFormat(), product.getId()));
                }
                file.setFormatId(formatId);
            }
        });
    }

}
