package fi.fmi.avi.archiver.config.model;

import static com.google.common.base.Preconditions.checkState;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.regex.Pattern;

import org.inferred.freebuilder.FreeBuilder;

import fi.fmi.avi.archiver.file.FilenameMatcher;
import fi.fmi.avi.model.GenericAviationWeatherMessage;

/**
 * Aviation product input file configuration model.
 */
@FreeBuilder
public abstract class FileConfig {
    FileConfig() {
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Return regular expression pattern of file names that are considered as input files.
     * Pattern may specify timestamp fields that {@link FilenameMatcher} can parse.
     *
     * <p>
     * Pattern must be non-empty and unique within input dir path throughout all {@link AviationProduct products}.
     * </p>
     *
     * @return input file pattern
     */
    public abstract Pattern getPattern();

    /**
     * Return time zone of a possible timestamp in input file name.
     * {@link ZoneOffset#UTC} should be used as default value, in case file name contains no timestamp.
     *
     * @return time zone of a possible timestamp in input file name
     */
    public abstract ZoneId getNameTimeZone();

    /**
     * Return format of messages in input file.
     * Different message formats cannot be mixed within a file.
     *
     * @return format of messages in input file
     */
    public abstract GenericAviationWeatherMessage.Format getFormat();

    /**
     * Return the numeric database identifier of message format.
     * This property is not set in the configuration file, but resolved automatically from {@link #getFormat() format name} at startup.
     *
     * @return format identifier
     */
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
