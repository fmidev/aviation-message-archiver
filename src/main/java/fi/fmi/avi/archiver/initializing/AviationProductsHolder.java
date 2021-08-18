package fi.fmi.avi.archiver.initializing;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

import java.io.File;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import org.inferred.freebuilder.FreeBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fi.fmi.avi.model.GenericAviationWeatherMessage;

/**
 * Holder for Aviation products.
 */
@Component
public class AviationProductsHolder {
    private final List<AviationProduct> products;

    public AviationProductsHolder(@Autowired final AviationProductBuildersHolder aviationProductBuildersHolder) {
        requireNonNull(aviationProductBuildersHolder, "aviationProductBuildersHolder");
        products = buildProducts(aviationProductBuildersHolder.getProducts());
    }

    public static <E> void iterateProducts(final List<E> productBuilders, final Consumer<? super E> productConsumer) {
        final int size = productBuilders.size();
        for (int i = 0; i < size; i++) {
            try {
                productConsumer.accept(productBuilders.get(i));
            } catch (final RuntimeException e) {
                throw new IllegalStateException("Product at index <" + i + "> is invalid", e);
            }
        }
    }

    private List<AviationProduct> buildProducts(final List<AviationProduct.Builder> productBuilders) {
        checkState(!productBuilders.isEmpty(), "Products are missing");
        final int size = productBuilders.size();
        final List<AviationProduct> builder = new ArrayList<>(size);
        iterateProducts(productBuilders, product -> builder.add(product.build()));
        return Collections.unmodifiableList(builder);
    }

    public List<AviationProduct> getProducts() {
        return products;
    }

    @FreeBuilder
    public abstract static class AviationProduct {
        AviationProduct() {
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

    @FreeBuilder
    public static abstract class FileConfig {
        FileConfig() {
        }

        public abstract Pattern getPattern();

        public abstract ZoneId getNameTimeZone();

        public abstract GenericAviationWeatherMessage.Format getFormat();

        public abstract int getFormatId();

        public static class Builder extends AviationProductsHolder_FileConfig_Builder {
            public Builder() {
            }

            @Override
            public FileConfig build() {
                checkState(!getPattern().toString().isEmpty(), "pattern is empty");
                return super.build();
            }
        }
    }
}
