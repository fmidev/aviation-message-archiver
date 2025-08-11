package fi.fmi.avi.archiver.config.model;

import fi.fmi.avi.archiver.message.processor.postaction.PostAction;
import fi.fmi.avi.archiver.util.instantiation.ForwardingObjectFactory;
import fi.fmi.avi.archiver.util.instantiation.ObjectFactory;

import static java.util.Objects.requireNonNull;

public class ForwardingPostActionFactory<T extends PostAction> extends ForwardingObjectFactory<T> implements PostActionFactory<T> {
    private final ObjectFactory<T> postActionFactory;

    private ForwardingPostActionFactory(final ObjectFactory<T> postActionFactory) {
        this.postActionFactory = postActionFactory;
    }

    public static <T extends PostAction> ForwardingPostActionFactory<T> create(final ObjectFactory<T> postActionFactory) {
        requireNonNull(postActionFactory, "postActionFactory");
        if (postActionFactory instanceof ForwardingPostActionFactory) {
            return (ForwardingPostActionFactory<T>) postActionFactory;
        } else if (postActionFactory instanceof AutoCloseable) {
            return new AutoCloseableFactory<>(postActionFactory, (AutoCloseable) postActionFactory);
        } else {
            return new ForwardingPostActionFactory<>(postActionFactory);
        }
    }

    public static <F extends ObjectFactory<T> & AutoCloseable, T extends PostAction> AutoCloseableFactory<T> createAutoCloseable(final F postActionFactory) {
        requireNonNull(postActionFactory, "postActionFactory");
        return new AutoCloseableFactory<>(postActionFactory, postActionFactory);
    }

    public static <T extends PostAction> AutoCloseableFactory<T> createAutoCloseable(final ObjectFactory<T> postActionFactory, final AutoCloseable closeable) {
        requireNonNull(postActionFactory, "postActionFactory");
        requireNonNull(closeable, "closeable");
        return new AutoCloseableFactory<>(postActionFactory, closeable);
    }

    @Override
    protected ObjectFactory<T> delegate() {
        return postActionFactory;
    }

    public static class AutoCloseableFactory<T extends PostAction> extends ForwardingPostActionFactory<T> implements AutoCloseable {
        final AutoCloseable closeable;

        private AutoCloseableFactory(final ObjectFactory<T> postActionFactory, final AutoCloseable closeable) {
            super(postActionFactory);
            this.closeable = closeable;
        }

        @Override
        public void close() throws Exception {
            closeable.close();
        }
    }
}
