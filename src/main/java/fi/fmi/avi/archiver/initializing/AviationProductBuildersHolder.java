package fi.fmi.avi.archiver.initializing;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import fi.fmi.avi.archiver.initializing.AviationProductsHolder.AviationProduct;
import fi.fmi.avi.archiver.initializing.AviationProductsHolder.FileConfig;
import fi.fmi.avi.model.GenericAviationWeatherMessage;

/**
 * Holder for Aviation product builders.
 * This is required to let Spring Boot populate values from application config.
 * {@link AviationProductsHolder} class holds the built immutable configuration objects.
 */
@Component
@ConfigurationProperties(prefix = "production-line-initialization")
class AviationProductBuildersHolder {

    @Resource(name = "messageRouteIds")
    private Map<String, Integer> messageRouteIds;
    @Resource(name = "messageFormatIds")
    private Map<GenericAviationWeatherMessage.Format, Integer> messageFormatIds;

    private List<AviationProduct.Builder> products = Collections.emptyList();

    public AviationProductBuildersHolder() {
    }

    public List<AviationProduct.Builder> getProducts() {
        return products;
    }

    public void setProducts(final List<AviationProduct.Builder> products) {
        this.products = products;
    }

    @PostConstruct
    private void mapRoutesToIds() {
        AviationProductsHolder.iterateProducts(products, product -> {
            final Integer routeId = messageRouteIds.get(product.getRoute());
            if (routeId == null) {
                throw new IllegalStateException(String.format(Locale.ROOT, "Unknown route <%s> for product <%s>", product.getRoute(), product.getId()));
            }
            product.setRouteId(routeId);
        });
    }

    @PostConstruct
    private void mapFormatsToIds() {
        AviationProductsHolder.iterateProducts(products, product -> {
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
