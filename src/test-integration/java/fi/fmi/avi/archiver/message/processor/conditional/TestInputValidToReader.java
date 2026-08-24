package fi.fmi.avi.archiver.message.processor.conditional;

import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessageOrBuilder;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.PartialOrCompleteTimePeriod;
import org.jspecify.annotations.Nullable;

import java.time.ZonedDateTime;

import static java.util.Objects.requireNonNull;

public class TestInputValidToReader extends AbstractConditionPropertyReader<ZonedDateTime> {
    @Override
    public @Nullable ZonedDateTime readValue(final InputAviationMessage input, final ArchiveAviationMessageOrBuilder target) {
        requireNonNull(input, "input");
        requireNonNull(target, "target");
        return input.getMessage().getValidityTime()
                .flatMap(PartialOrCompleteTimePeriod::getEndTime)
                .flatMap(PartialOrCompleteTimeInstant::getCompleteTime)
                .orElse(null);
    }
}
