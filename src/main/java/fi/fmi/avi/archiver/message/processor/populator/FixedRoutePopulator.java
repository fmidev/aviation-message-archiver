package fi.fmi.avi.archiver.message.processor.populator;

import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.processor.MessageProcessorContext;

import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

/**
 * Set a fixed {@link ArchiveAviationMessage#getRoute() route} on message.
 * This populator is typically composed as {@link ConditionalMessagePopulator} to limit affected messages.
 */
public class FixedRoutePopulator implements MessagePopulator {

    private final int route;

    public FixedRoutePopulator(final Map<String, Integer> routeIds, final String route) {
        requireNonNull(routeIds, "routeIds");
        requireNonNull(route, "route");
        checkArgument(routeIds.containsKey(route), "No routeId exist for route <" + route + ">");
        this.route = routeIds.get(route);
    }

    @Override
    public void populate(final MessageProcessorContext context, final ArchiveAviationMessage.Builder target) {
        requireNonNull(context, "context");
        requireNonNull(target, "target");
        target.setRoute(route);
    }
}
