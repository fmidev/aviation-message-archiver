package fi.fmi.avi.archiver.logging.logback;

import ch.qos.logback.core.Appender;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.spi.AppenderAttachable;
import org.jspecify.annotations.Nullable;

import java.util.Iterator;

import static java.util.Objects.requireNonNull;

public abstract class ForwardingAppenderBase<E> extends AppenderBase<E> implements AppenderAttachable<E> {
    protected abstract AppenderAttachable<E> appenders();

    @Override
    protected void append(final @Nullable E eventObject) {
        if (!isStarted() || eventObject == null) {
            return;
        }
        for (final Iterator<Appender<E>> iterator = appenders().iteratorForAppenders(); iterator.hasNext(); ) {
            iterator.next().doAppend(eventObject);
        }
    }

    @Override
    public void start() {
        startAttachedAppenders();
        super.start();
    }

    private void startAttachedAppenders() {
        for (final Iterator<Appender<E>> iterator = appenders().iteratorForAppenders(); iterator.hasNext(); ) {
            final Appender<E> appender = iterator.next();
            if (appender.getContext() == null) {
                appender.setContext(getContext());
            }
            if (!appender.isStarted()) {
                appender.start();
            }
        }
    }

    @Override
    public void stop() {
        super.stop();
        stopAttachedAppenders();
    }

    private void stopAttachedAppenders() {
        for (final Iterator<Appender<E>> iterator = appenders().iteratorForAppenders(); iterator.hasNext(); ) {
            final Appender<E> appender = iterator.next();
            if (appender.isStarted()) {
                appender.stop();
            }
        }
    }

    @Override
    public void addAppender(final Appender<E> newAppender) {
        appenders().addAppender(requireNonNull(newAppender, "newAppender"));
    }

    @Override
    public Iterator<Appender<E>> iteratorForAppenders() {
        return appenders().iteratorForAppenders();
    }

    @Override
    public @Nullable Appender<E> getAppender(final @Nullable String name) {
        return appenders().getAppender(name);
    }

    @Override
    public boolean isAttached(final @Nullable Appender<E> appender) {
        return appenders().isAttached(appender);
    }

    @Override
    public void detachAndStopAllAppenders() {
        appenders().detachAndStopAllAppenders();
    }

    @Override
    public boolean detachAppender(final @Nullable Appender<E> appender) {
        return appenders().detachAppender(appender);
    }

    @Override
    public boolean detachAppender(final @Nullable String name) {
        return appenders().detachAppender(name);
    }
}
