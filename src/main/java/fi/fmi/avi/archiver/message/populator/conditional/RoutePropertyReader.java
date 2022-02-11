package fi.fmi.avi.archiver.message.populator.conditional;

import static java.util.Objects.requireNonNull;

import javax.annotation.Nullable;

import com.google.common.collect.BiMap;

import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.populator.MessagePopulatorHelper;

public class RoutePropertyReader extends AbstractConditionPropertyReader<String> {
    private final BiMap<String, Integer> messageRouteIds;

    public RoutePropertyReader(final BiMap<String, Integer> messageRouteIds) {
        this.messageRouteIds = requireNonNull(messageRouteIds, "messageRouteIds");
    }

    @Nullable
    @Override
    public String readValue(final InputAviationMessage input, final ArchiveAviationMessage.Builder target) {
        requireNonNull(input, "input");
        requireNonNull(target, "target");
        final int routeId = MessagePopulatorHelper.tryGetInt(target, builder -> builder.getRoute()).orElse(Integer.MIN_VALUE);
        return messageRouteIds.inverse().get(routeId);
    }

    @Override
    public boolean validate(final String value) {
        requireNonNull(value, "value");
        return messageRouteIds.containsKey(value);
    }
}
