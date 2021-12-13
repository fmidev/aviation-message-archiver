package fi.fmi.avi.archiver.initializing;

import com.google.common.collect.ImmutableMap;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

@Configuration
public class AviationProductConfig {

    @Bean
    public Map<String, AviationProduct> aviationProducts(final AviationProductBuildersHolder aviationProductBuildersHolder) {
        return buildProducts(aviationProductBuildersHolder.getProducts());
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

    private Map<String, AviationProduct> buildProducts(final List<AviationProduct.Builder> productBuilders) {
        checkState(!productBuilders.isEmpty(), "Products are missing");
        final ImmutableMap.Builder<String, AviationProduct> builder = ImmutableMap.builder();
        iterateProducts(productBuilders, productBuilder -> {
            final AviationProduct product = productBuilder.build();
            builder.put(product.getId(), product);
        });
        return builder.build();
    }

}
