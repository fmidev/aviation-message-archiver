package fi.fmi.avi.archiver.message.processor.conditional;

import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessageOrBuilder;
import fi.fmi.avi.archiver.message.processor.MessageProcessorHelper;

import javax.annotation.Nullable;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;

public class StationPropertyReader extends AbstractConditionPropertyReader<String> {
    private static final Pattern ICAO_CODE_PATTERN = Pattern.compile("^[A-Z]{4}$");

    @Nullable
    @Override
    public String readValue(final InputAviationMessage input, final ArchiveAviationMessageOrBuilder target) {
        requireNonNull(input, "input");
        requireNonNull(target, "target");
        return MessageProcessorHelper.tryGet(target, ArchiveAviationMessageOrBuilder::getStationIcaoCode)//
                .orElse(null);
    }

    @Override
    public boolean validate(final String value) {
        requireNonNull(value, "value");
        return ICAO_CODE_PATTERN.matcher(value).matches();
    }
}
