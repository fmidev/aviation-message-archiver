package fi.fmi.avi.archiver.message.populator;

import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;

import javax.annotation.Nullable;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;

/**
 * Replace ICAO airport code using a regex pattern.
 */
public class IcaoAirportCodeReplacer implements MessagePopulator {

    private Pattern pattern;
    private String replacement;

    public void setPattern(final Pattern pattern) {
        this.pattern = requireNonNull(pattern, "pattern");
    }

    public void setReplacement(final String replacement) {
        this.replacement = requireNonNull(replacement, "replacement");
    }

    @Override
    public void populate(@Nullable final InputAviationMessage inputAviationMessage, final ArchiveAviationMessage.Builder builder) {
        requireNonNull(builder, "builder");
        final String airportCode = pattern.matcher(builder.getIcaoAirportCode()).replaceAll(replacement);
        builder.setIcaoAirportCode(airportCode);
    }

}
