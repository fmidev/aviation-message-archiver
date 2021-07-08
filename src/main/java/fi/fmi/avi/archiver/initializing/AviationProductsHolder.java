package fi.fmi.avi.archiver.initializing;

import static com.google.common.base.Preconditions.checkState;

import java.io.File;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;

import fi.fmi.avi.model.GenericAviationWeatherMessage;

/**
 * Holder for Aviation products.
 */
@Component
@ConfigurationProperties(prefix = "production-line-initialization")
public class AviationProductsHolder {

    @Resource(name = "messageRouteIds")
    private Map<String, Integer> messageRouteIds;
    @Resource(name = "messageFormatIds")
    private Map<GenericAviationWeatherMessage.Format, Integer> messageFormatIds;

    private List<AviationProduct> products = Collections.emptyList();

    public AviationProductsHolder() {
    }

    public AviationProductsHolder(final List<AviationProduct> products) {
        this.products = Collections.unmodifiableList(products);
    }

    public List<AviationProduct> getProducts() {
        return Collections.unmodifiableList(products);
    }

    public void setProducts(final List<AviationProduct> products) {
        this.products = Collections.unmodifiableList(products);
    }

    @PostConstruct
    private void mapRoutesToIds() {
        for (final AviationProduct product : products) {
            final Integer routeId = messageRouteIds.get(product.getRoute());
            if (routeId == null) {
                throw new IllegalStateException(String.format(Locale.ROOT, "Unknown route <%s> for product <%s>", product.getRoute(), product.getId()));
            }
            product.setRouteId(routeId);
        }
    }

    @PostConstruct
    private void mapFormatsToIds() {
        for (final AviationProduct product : products) {
            for (final FileConfig file : product.getFiles()) {
                final Integer formatId = messageFormatIds.get(file.getFormat());
                if (formatId == null) {
                    throw new IllegalStateException(
                            String.format(Locale.ROOT, "Unknown file message format <%s> for product <%s>", file.getFormat(), product.getId()));
                }
                file.setFormatId(formatId);
            }
        }
    }

    @PostConstruct
    private void checkProductPropertiesHaveValues() {
        for (int productIndex = 0; productIndex < products.size(); productIndex++) {
            final AviationProduct product = products.get(productIndex);
            checkState(Strings.emptyToNull(product.getId()) != null, "Product at index <%s> is missing id", productIndex);
            checkState(Strings.emptyToNull(product.getRoute()) != null, "Product <%s> is missing route", product.getId());
            checkState(product.getInputDir() != null, "Product <%s> is missing inputDir", product.getId());
            checkState(product.getArchiveDir() != null, "Product <%s> is missing archiveDir", product.getId());
            checkState(product.getFailDir() != null, "Product <%s> is missing failDir", product.getId());
            final List<FileConfig> files = product.getFiles();
            checkState(!files.isEmpty(), "Product <%s> is missing files", product.getId());
            for (int fileIndex = 0; fileIndex < files.size(); fileIndex++) {
                final FileConfig file = files.get(fileIndex);
                checkState(file.getPattern() != null && !file.getPattern().toString().isEmpty(), "Product <%s> file config at index <%s> is missing pattern",
                        product.getId(), fileIndex);
                checkState(file.getNameTimeZone() != null, "Product <%s> file config at index <%s> is missing nameTimeZone", product.getId(), fileIndex);
                checkState(file.getFormat() != null, "Product <%s> file config at index <%s> is missing format", product.getId(), fileIndex);
            }
        }
    }

    /**
     * Aviation product.
     */
    public static class AviationProduct {
        private String id;
        private String route;
        private int routeId = -1;
        private File inputDir;
        private File archiveDir;
        private File failDir;
        private List<FileConfig> files = Collections.emptyList();

        public AviationProduct() {
        }

        public AviationProduct(final String id, final String route, final int routeId, final File inputDir, final File archiveDir, final File failDir,
                final List<FileConfig> files) {
            this.id = id;
            this.route = route;
            this.routeId = routeId;
            this.inputDir = inputDir;
            this.archiveDir = archiveDir;
            this.failDir = failDir;
            this.files = Collections.unmodifiableList(files);
        }

        public String getId() {
            return id;
        }

        public void setId(final String id) {
            this.id = id;
        }

        public String getRoute() {
            return route;
        }

        public void setRoute(final String route) {
            this.route = route;
        }

        public int getRouteId() {
            return routeId;
        }

        public void setRouteId(final int routeId) {
            this.routeId = routeId;
        }

        public File getInputDir() {
            return inputDir;
        }

        public void setInputDir(final File inputDir) {
            this.inputDir = inputDir;
        }

        public File getArchiveDir() {
            return archiveDir;
        }

        public void setArchiveDir(final File archiveDir) {
            this.archiveDir = archiveDir;
        }

        public File getFailDir() {
            return failDir;
        }

        public void setFailDir(final File failDir) {
            this.failDir = failDir;
        }

        public List<FileConfig> getFiles() {
            return Collections.unmodifiableList(files);
        }

        public void setFiles(final List<FileConfig> files) {
            this.files = Collections.unmodifiableList(files);
        }
    }

    public static class FileConfig {
        private Pattern pattern;
        private ZoneId nameTimeZone = ZoneOffset.UTC;
        private GenericAviationWeatherMessage.Format format;
        private int formatId = -1;

        public FileConfig() {
        }

        public FileConfig(final Pattern pattern, final ZoneId nameTimeZone, final GenericAviationWeatherMessage.Format format, final int formatId) {
            this.pattern = pattern;
            this.nameTimeZone = nameTimeZone;
            this.format = format;
            this.formatId = formatId;
        }

        public Pattern getPattern() {
            return pattern;
        }

        public void setPattern(final Pattern pattern) {
            this.pattern = pattern;
        }

        public ZoneId getNameTimeZone() {
            return nameTimeZone;
        }

        public void setNameTimeZone(final ZoneId nameTimeZone) {
            this.nameTimeZone = nameTimeZone;
        }

        public GenericAviationWeatherMessage.Format getFormat() {
            return format;
        }

        public void setFormat(final GenericAviationWeatherMessage.Format format) {
            this.format = format;
        }

        public int getFormatId() {
            return formatId;
        }

        public void setFormatId(final int formatId) {
            this.formatId = formatId;
        }
    }
}
