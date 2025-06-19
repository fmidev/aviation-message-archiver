package fi.fmi.avi.archiver.message.processor.conditional;

import com.google.common.collect.BiMap;
import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessageOrBuilder;
import fi.fmi.avi.archiver.message.processor.MessageProcessorHelper;
import fi.fmi.avi.model.GenericAviationWeatherMessage;

import javax.annotation.Nullable;

import static java.util.Objects.requireNonNull;

public class FormatPropertyReader extends AbstractConditionPropertyReader<GenericAviationWeatherMessage.Format> {
    private final BiMap<GenericAviationWeatherMessage.Format, Integer> messageFormatIds;

    public FormatPropertyReader(final BiMap<GenericAviationWeatherMessage.Format, Integer> messageFormatIds) {
        this.messageFormatIds = requireNonNull(messageFormatIds, "messageFormatIds");
    }

    @Nullable
    @Override
    public GenericAviationWeatherMessage.Format readValue(final InputAviationMessage input, final ArchiveAviationMessageOrBuilder target) {
        requireNonNull(input, "input");
        requireNonNull(target, "target");
        final int formatId = MessageProcessorHelper.tryGetInt(target, ArchiveAviationMessageOrBuilder::getFormat).orElse(Integer.MIN_VALUE);
        return messageFormatIds.inverse().get(formatId);
    }

    @Override
    public boolean validate(final GenericAviationWeatherMessage.Format value) {
        requireNonNull(value, "value");
        return messageFormatIds.containsKey(value);
    }
}
