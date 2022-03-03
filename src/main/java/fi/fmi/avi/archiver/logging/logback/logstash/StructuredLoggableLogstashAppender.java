package fi.fmi.avi.archiver.logging.logback.logstash;

import static java.util.Objects.requireNonNull;

import javax.annotation.Nullable;

import net.logstash.logback.argument.StructuredArguments;

import com.google.common.annotations.VisibleForTesting;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.spi.AppenderAttachable;
import ch.qos.logback.core.spi.AppenderAttachableImpl;
import fi.fmi.avi.archiver.logging.StructuredLoggable;
import fi.fmi.avi.archiver.logging.logback.ForwardingAppenderBase;
import fi.fmi.avi.archiver.logging.logback.ForwardingLoggingEvent;

public class StructuredLoggableLogstashAppender extends ForwardingAppenderBase<ILoggingEvent> {
    private final AppenderAttachableImpl<ILoggingEvent> appenders;

    public StructuredLoggableLogstashAppender() {
        this(new AppenderAttachableImpl<>());
    }

    @VisibleForTesting
    StructuredLoggableLogstashAppender(final AppenderAttachableImpl<ILoggingEvent> appenders) {
        this.appenders = requireNonNull(appenders, "appenders");
    }

    @Override
    protected AppenderAttachable<ILoggingEvent> appenders() {
        return appenders;
    }

    @Override
    protected void append(@Nullable final ILoggingEvent eventObject) {
        if (!isStarted() || eventObject == null) {
            return;
        }
        appenders.appendLoopOnAppenders(new LoggingEvent(eventObject));
    }

    @VisibleForTesting
    @SuppressWarnings("TransientFieldInNonSerializableClass")
    static final class LoggingEvent extends ForwardingLoggingEvent {
        private final ILoggingEvent delegate;

        @Nullable
        private transient Object[] argumentArray;
        private transient boolean argumentArrayPreparedForDeferredProcessing;

        LoggingEvent(final ILoggingEvent delegate) {
            this.delegate = requireNonNull(delegate, "delegate");
        }

        @Override
        protected ILoggingEvent delegate() {
            return delegate;
        }

        @Nullable
        @Override
        public Object[] getArgumentArray() {
            return getArgumentArray(false);
        }

        @Nullable
        private Object[] getArgumentArray(final boolean prepareForDeferredProcessing) {
            if (!argumentArrayPreparedForDeferredProcessing && (prepareForDeferredProcessing || argumentArray == null)) {
                // Above condition ensures this is invoked at maximum twice:
                // 1. first time when prepareForDeferredProcessing == false and
                // 2. first time when prepareForDeferredProcessing == true, and never after
                initArgumentArray(prepareForDeferredProcessing);
            }
            return argumentArray;
        }

        private void initArgumentArray(final boolean copyStructuredLoggables) {
            @Nullable
            final Object[] delegateArgumentArray = delegate().getArgumentArray();
            if (delegateArgumentArray == null) {
                argumentArray = null;
                argumentArrayPreparedForDeferredProcessing = true;
                return;
            }
            if (argumentArray == null) {
                argumentArray = delegateArgumentArray.clone();
                assert argumentArray != null : "argumentArray should be non-null";
            }
            for (int i = 0; i < delegateArgumentArray.length; i++) {
                if (delegateArgumentArray[i] instanceof StructuredLoggable) {
                    final StructuredLoggable structuredLoggable = copyStructuredLoggables
                            ? ((StructuredLoggable) delegateArgumentArray[i]).readableCopy()
                            : (StructuredLoggable) delegateArgumentArray[i];
                    argumentArray[i] = StructuredArguments.keyValue(structuredLoggable.getStructureName(), structuredLoggable);
                }
            }
            argumentArrayPreparedForDeferredProcessing |= copyStructuredLoggables;
        }

        @Override
        public void prepareForDeferredProcessing() {
            super.prepareForDeferredProcessing();
            getArgumentArray(true);
        }
    }
}
