package fi.fmi.avi.archiver.logging.logback;

import static java.util.Objects.requireNonNull;

import java.util.Iterator;

import javax.annotation.Nullable;

import ch.qos.logback.core.Appender;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.spi.AppenderAttachable;

public abstract class ForwardingAppenderBase<E> extends AppenderBase<E> implements AppenderAttachable<E> {
    protected abstract AppenderAttachable<E> appenders();

    @Override
    protected void append(@Nullable final E eventObject) {
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

    @Nullable
    @Override
    public Appender<E> getAppender(@Nullable final String name) {
        return appenders().getAppender(name);
    }

    @Override
    public boolean isAttached(@Nullable final Appender<E> appender) {
        return appenders().isAttached(appender);
    }

    @Override
    public void detachAndStopAllAppenders() {
        appenders().detachAndStopAllAppenders();
    }

    @Override
    public boolean detachAppender(@Nullable final Appender<E> appender) {
        return appenders().detachAppender(appender);
    }

    @Override
    public boolean detachAppender(@Nullable final String name) {
        return appenders().detachAppender(name);
    }
}
