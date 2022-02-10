package fi.fmi.avi.archiver.message.populator.conditional;

import static java.util.Objects.requireNonNull;

import java.util.regex.Pattern;

import javax.annotation.Nullable;

import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.populator.MessagePopulatorHelper;

public class StationPropertyReader extends AbstractConditionPropertyReader<String> {
    private static final Pattern ICAO_CODE_PATTERN = Pattern.compile("^[A-Z]{4}$");

    @Nullable
    @Override
    public String readValue(final InputAviationMessage input, final ArchiveAviationMessage.Builder target) {
        requireNonNull(input, "input");
        requireNonNull(target, "target");
        return MessagePopulatorHelper.tryGet(target, builder -> builder.getStationIcaoCode())//
                .orElse(null);
    }

    @Override
    public boolean validate(final String value) {
        requireNonNull(value, "value");
        return ICAO_CODE_PATTERN.matcher(value).matches();
    }
}
