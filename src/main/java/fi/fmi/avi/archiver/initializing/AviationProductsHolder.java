package fi.fmi.avi.archiver.initializing;

import com.google.common.collect.ImmutableMap;
import fi.fmi.avi.archiver.file.FileConfig;
import org.inferred.freebuilder.FreeBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

/**
 * Holder for Aviation products.
 */
@Component
public class AviationProductsHolder {
    private final Map<String, AviationProduct> products;

    public AviationProductsHolder(@Autowired final AviationProductBuildersHolder aviationProductBuildersHolder) {
        requireNonNull(aviationProductBuildersHolder, "aviationProductBuildersHolder");
        products = buildProducts(aviationProductBuildersHolder.getProducts());
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

    /**
     * Returns products indexed by product id (returned by {@link AviationProduct#getId()}).
     *
     * @return products
     */
    public Map<String, AviationProduct> getProducts() {
        return products;
    }

    @FreeBuilder
    public abstract static class AviationProduct {
        AviationProduct() {
        }

        public static Builder builder() {
            return new Builder();
        }

        public abstract String getId();

        public abstract String getRoute();

        public abstract int getRouteId();

        public abstract File getInputDir();

        public abstract File getArchiveDir();

        public abstract File getFailDir();

        public abstract List<FileConfig> getFileConfigs();

        public static class Builder extends AviationProductsHolder_AviationProduct_Builder {
            public Builder() {
            }

            @Override
            public AviationProduct build() {
                checkState(!getId().isEmpty(), "id is empty");
                checkState(!getRoute().isEmpty(), "route is empty");
                checkState(!getBuildersOfFileConfigs().isEmpty(), "fileConfigs (files) is empty");
                return super.build();
            }

            public List<FileConfig.Builder> getFiles() {
                return getBuildersOfFileConfigs();
            }

            public Builder setFiles(final List<FileConfig.Builder> files) {
                return clearFileConfigs().addAllBuildersOfFileConfigs(files);
            }
        }
    }

}
