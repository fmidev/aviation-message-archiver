package fi.fmi.avi.archiver.message.populator.conditional;

import static java.util.Objects.requireNonNull;

import javax.annotation.Nullable;

import com.google.common.collect.BiMap;

import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.populator.MessagePopulatorHelper;
import fi.fmi.avi.model.GenericAviationWeatherMessage;

public class FormatPropertyReader extends AbstractConditionPropertyReader<GenericAviationWeatherMessage.Format> {
    private final BiMap<GenericAviationWeatherMessage.Format, Integer> messageFormatIds;

    public FormatPropertyReader(final BiMap<GenericAviationWeatherMessage.Format, Integer> messageFormatIds) {
        this.messageFormatIds = requireNonNull(messageFormatIds, "messageFormatIds");
    }

    @Nullable
    @Override
    public GenericAviationWeatherMessage.Format readValue(final InputAviationMessage input, final ArchiveAviationMessage.Builder target) {
        requireNonNull(input, "input");
        requireNonNull(target, "target");
        final int formatId = MessagePopulatorHelper.tryGetInt(target, builder -> builder.getFormat()).orElse(Integer.MIN_VALUE);
        return messageFormatIds.inverse().get(formatId);
    }

    @Override
    public boolean validate(final GenericAviationWeatherMessage.Format value) {
        requireNonNull(value, "value");
        return messageFormatIds.containsKey(value);
    }
}
