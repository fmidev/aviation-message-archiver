package fi.fmi.avi.archiver.initializing;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Holder for Aviation products.
 */
@Component
@ConfigurationProperties(prefix = "production-line-initialization")
public class AviationProductsHolder {

    private Set<AviationProduct> products = Collections.emptySet();

    public AviationProductsHolder() {
    }

    public AviationProductsHolder(final Set<AviationProduct> products) {
        this.products = Collections.unmodifiableSet(products);
    }

    public Set<AviationProduct> getProducts() {
        return Collections.unmodifiableSet(products);
    }

    public void setProducts(final Set<AviationProduct> products) {
        this.products = Collections.unmodifiableSet(products);
    }

    /**
     * Aviation product.
     */
    public static class AviationProduct {
        private String id;
        private File inputDir;
        private File archiveDir;
        private File failDir;
        private Set<FileConfig> files = Collections.emptySet();

        public AviationProduct() {
        }

        public AviationProduct(final String id, final File inputDir, final File archiveDir, final File failDir, final Set<FileConfig> files) {
            this.id = id;
            this.inputDir = inputDir;
            this.archiveDir = archiveDir;
            this.failDir = failDir;
            this.files = Collections.unmodifiableSet(files);
        }

        public String getId() {
            return id;
        }

        public void setId(final String id) {
            this.id = id;
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

        public Set<FileConfig> getFiles() {
            return Collections.unmodifiableSet(files);
        }

        public void setFiles(final Set<FileConfig> files) {
            this.files = Collections.unmodifiableSet(files);
        }

        @Override
        public boolean equals(final Object obj) {
            if (!(obj instanceof AviationProduct)) {
                return false;
            }
            final AviationProduct other = (AviationProduct) obj;
            return Objects.equals(id, other.id) && Objects.equals(inputDir, other.getInputDir()) && Objects.equals(archiveDir, other.getArchiveDir())
                    && Objects.equals(failDir, other.getFailDir()) && Objects.equals(files, other.getFiles());
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, inputDir, archiveDir, failDir);
        }
    }

    public static class FileConfig {
        private String pattern;
        private Pattern compiledPattern;

        public FileConfig() {
        }

        public FileConfig(final String pattern) {
            this.pattern = pattern;
        }

        public String getPattern() {
            return pattern;
        }

        public void setPattern(final String pattern) {
            this.pattern = pattern;
        }

        public Pattern getCompiledPattern() {
            if (compiledPattern == null) {
                compiledPattern = Pattern.compile(pattern);
            }
            return compiledPattern;
        }

        @Override
        public boolean equals(final Object obj) {
            if (!(obj instanceof FileConfig)) {
                return false;
            }
            final FileConfig other = (FileConfig) obj;
            return Objects.equals(pattern, other.getPattern());
        }

        @Override
        public int hashCode() {
            return Objects.hash(pattern);
        }
    }
}
