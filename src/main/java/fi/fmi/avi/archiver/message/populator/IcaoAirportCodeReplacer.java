package fi.fmi.avi.archiver.message.populator;

import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;

import javax.annotation.Nullable;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

/**
 * Replace ICAO airport code using a regex pattern.
 */
public class IcaoAirportCodeReplacer implements MessagePopulator {

    private final Pattern pattern;
    private final String replacement;

    public IcaoAirportCodeReplacer(final Pattern pattern, final String replacement) {
        this.pattern = requireNonNull(pattern, "pattern");
        requireNonNull(replacement, "replacement");
        checkArgument(!replacement.isEmpty(), "replacement cannot be empty");
        this.replacement = replacement;
    }

    @Override
    public void populate(@Nullable final InputAviationMessage inputAviationMessage, final ArchiveAviationMessage.Builder builder) {
        requireNonNull(builder, "builder");
        final String airportCode = pattern.matcher(builder.getIcaoAirportCode()).replaceAll(replacement);
        builder.setIcaoAirportCode(airportCode);
    }

}
