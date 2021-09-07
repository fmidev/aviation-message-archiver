package fi.fmi.avi.archiver.file;

import static com.google.common.base.Preconditions.checkState;

import java.time.ZoneId;
import java.util.regex.Pattern;

import org.inferred.freebuilder.FreeBuilder;

import fi.fmi.avi.model.GenericAviationWeatherMessage;

@FreeBuilder
public abstract class FileConfig {
    FileConfig() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public abstract Pattern getPattern();

    public abstract ZoneId getNameTimeZone();

    public abstract GenericAviationWeatherMessage.Format getFormat();

    public abstract int getFormatId();

    public abstract Builder toBuilder();

    public static class Builder extends FileConfig_Builder {
        Builder() {
        }

        @Override
        public FileConfig build() {
            checkState(!getPattern().toString().isEmpty(), "pattern is empty");
            return super.build();
        }
    }
}
