package fi.fmi.avi.archiver.message.populator;

import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.MessageProcessorContext;
import fi.fmi.avi.archiver.message.populator.conditional.ConditionalMessagePopulator;

import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

/**
 * Replace station ICAO code using a regex pattern.
 * All pattern matches are replaced.
 * This populator is typically composed as {@link ConditionalMessagePopulator} to limit affected messages.
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
    public void populate(final MessageProcessorContext context, final ArchiveAviationMessage.Builder target) {
        requireNonNull(context, "context");
        requireNonNull(target, "target");
        MessagePopulatorHelper.tryGet(target, reader -> reader.getStationIcaoCode()).ifPresent(icaoCode -> {
            final String stationIcaoCode = pattern.matcher(icaoCode).replaceAll(replacement);
            target.setStationIcaoCode(stationIcaoCode);
        });
    }

}
