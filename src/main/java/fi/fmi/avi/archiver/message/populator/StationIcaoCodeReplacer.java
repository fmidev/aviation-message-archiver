package fi.fmi.avi.archiver.message.populator;

import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;

import javax.annotation.Nullable;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

/**
 * Replace station ICAO code using a regex pattern.
 */
public class StationIcaoCodeReplacer implements MessagePopulator {

    private final Pattern pattern;
    private final String replacement;

    public StationIcaoCodeReplacer(final Pattern pattern, final String replacement) {
        this.pattern = requireNonNull(pattern, "pattern");
        requireNonNull(replacement, "replacement");
        checkArgument(!replacement.isEmpty(), "replacement cannot be empty");
        this.replacement = replacement;
    }

    @Override
    public void populate(@Nullable final InputAviationMessage inputAviationMessage, final ArchiveAviationMessage.Builder builder) {
        requireNonNull(builder, "builder");
        MessagePopulatorHelper.tryGet(builder, reader -> reader.getStationIcaoCode()).ifPresent(icaoCode -> {
            final String stationIcaoCode = pattern.matcher(icaoCode).replaceAll(replacement);
            builder.setStationIcaoCode(stationIcaoCode);
        });
    }

}
