package fi.fmi.avi.archiver.initializing;

import java.util.Collections;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import reactor.util.annotation.Nullable;

@Component
@ConfigurationProperties(prefix = "file-type-initialization")
public class AviFileTypeHolder {

    private Set<AviFileTypeConfiguration> types = Collections.emptySet();

    public AviFileTypeHolder() {
    }

    public AviFileTypeHolder(final Set<AviFileTypeConfiguration> types) {
        this.types = Collections.unmodifiableSet(types);
    }

    public Set<AviFileTypeConfiguration> getTypes() {
        return Collections.unmodifiableSet(types);
    }

    @Nullable
    public void setTypes(@Nullable final Set<AviFileTypeConfiguration> types) {
        if (types != null) {
            this.types = Collections.unmodifiableSet(types);
        }
    }

    public static class AviFileTypeConfiguration {
        private String type;
        private String pattern;

        public AviFileTypeConfiguration() {
        }

        public AviFileTypeConfiguration(final String type, final String pattern) {
            this.type = type;
            this.pattern = pattern;
        }

        public String getType() {
            return type;
        }

        public void setType(final String type) {
            this.type = type;
        }

        public String getPattern() {
            return pattern;
        }

        public void setPattern(final String pattern) {
            this.pattern = pattern;
        }
    }
}
