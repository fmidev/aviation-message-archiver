package fi.fmi.avi.archiver.config.model;

import fi.fmi.avi.archiver.message.postaction.PostAction;
import fi.fmi.avi.archiver.util.instantiation.ForwardingObjectFactory;
import fi.fmi.avi.archiver.util.instantiation.ObjectFactory;

import static java.util.Objects.requireNonNull;

/**
 * A typed {@code ObjectFactory} wrapper to ease configuration by type.
 *
 * @param <T> type of post-action
 */
public class PostActionFactory<T extends PostAction> extends ForwardingObjectFactory<T> {
    private final ObjectFactory<T> postActionFactory;

    public PostActionFactory(final ObjectFactory<T> postActionFactory) {
        this.postActionFactory = requireNonNull(postActionFactory, "postActionFactory");
    }

    @Override
    protected ObjectFactory<T> delegate() {
        return postActionFactory;
    }
}
