package fi.fmi.avi.archiver.initializing;

import fi.fmi.avi.archiver.file.FileConfig;
import fi.fmi.avi.model.GenericAviationWeatherMessage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

/**
 * Holder for Aviation product builders.
 * This is required to let Spring Boot populate values from application config.
 * {@link AviationProductConfig} class holds the built immutable configuration objects.
 */
@Component
@ConfigurationProperties(prefix = "production-line-initialization")
class AviationProductBuildersHolder {

    private final Map<String, Integer> messageRouteIds;
    private final Map<GenericAviationWeatherMessage.Format, Integer> messageFormatIds;

    private List<AviationProduct.Builder> products = Collections.emptyList();

    public AviationProductBuildersHolder(@Qualifier("messageRouteIds") final Map<String, Integer> messageRouteIds,
                                         @Qualifier("messageFormatIds") final Map<GenericAviationWeatherMessage.Format, Integer> messageFormatIds) {
        this.messageRouteIds = requireNonNull(messageRouteIds, "messageRouteIds");
        this.messageFormatIds = requireNonNull(messageFormatIds, "messageFormatIds");

        checkArgument(!messageRouteIds.isEmpty(), "messageRouteIds cannot be empty");
        checkArgument(!messageFormatIds.isEmpty(), "messageFormatIds cannot be empty");
    }

    public List<AviationProduct.Builder> getProducts() {
        return products;
    }

    public void setProducts(final List<AviationProduct.Builder> products) {
        this.products = requireNonNull(products, "products");
    }

    @PostConstruct
    private void mapRoutesToIds() {
        AviationProductConfig.iterateProducts(products, product -> {
            final Integer routeId = messageRouteIds.get(product.getRoute());
            if (routeId == null) {
                throw new IllegalStateException(String.format(Locale.ROOT, "Unknown route <%s> for product <%s>", product.getRoute(), product.getId()));
            }
            product.setRouteId(routeId);
        });
    }

    @PostConstruct
    private void mapFormatsToIds() {
        AviationProductConfig.iterateProducts(products, product -> {
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
