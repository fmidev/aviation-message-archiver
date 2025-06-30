package fi.fmi.avi.archiver.config.model;

import fi.fmi.avi.archiver.message.processor.postaction.PostAction;
import fi.fmi.avi.archiver.util.instantiation.ForwardingObjectFactory;
import fi.fmi.avi.archiver.util.instantiation.ObjectFactory;

import static java.util.Objects.requireNonNull;

public class ForwardingPostActionFactory<T extends PostAction> extends ForwardingObjectFactory<T> implements PostActionFactory<T> {
    private final ObjectFactory<T> postActionFactory;

    public ForwardingPostActionFactory(final ObjectFactory<T> postActionFactory) {
        this.postActionFactory = requireNonNull(postActionFactory, "postActionFactory");
    }

    @Override
    protected ObjectFactory<T> delegate() {
        return postActionFactory;
    }
}
