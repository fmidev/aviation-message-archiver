package fi.fmi.avi.archiver.config;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import reactor.util.annotation.Nullable;

@Component
@ConfigurationProperties(prefix = "file-type-initialization")
public class FileTypeHolder {

    private Set<FileTypeConfiguration> types = new HashSet<>();

    public Set<FileTypeConfiguration> getTypes() {
        return Collections.unmodifiableSet(types);
    }

    @Nullable
    public void setTypes(@Nullable final Set<FileTypeConfiguration> types) {
        if (types != null) {
            this.types = Collections.unmodifiableSet(types);
        }
    }

    public static class FileTypeConfiguration {
        private String type;
        private String pattern;

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
