package fi.fmi.avi.archiver.message.populator;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import java.util.Map;

import fi.fmi.avi.archiver.message.ArchiveAviationMessage;

/**
 * Set a fixed route on message.
 * Use within {@link fi.fmi.avi.archiver.message.populator.conditional.ConditionalMessagePopulator ConditionalMessagePopulator} to select affected messages.
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
    public void populate(final MessagePopulatingContext context, final ArchiveAviationMessage.Builder target) {
        requireNonNull(context, "context");
        requireNonNull(target, "target");
        target.setRoute(route);
    }
}
