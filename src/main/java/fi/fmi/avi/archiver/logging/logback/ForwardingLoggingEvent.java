package fi.fmi.avi.archiver.logging.logback;

import java.util.Map;

import org.slf4j.Marker;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.LoggerContextVO;

public abstract class ForwardingLoggingEvent implements ILoggingEvent {
    protected abstract ILoggingEvent delegate();

    @Override
    public String getThreadName() {
        return delegate().getThreadName();
    }

    @Override
    public Level getLevel() {
        return delegate().getLevel();
    }

    @Override
    public String getMessage() {
        return delegate().getMessage();
    }

    @Override
    public Object[] getArgumentArray() {
        return delegate().getArgumentArray();
    }

    @Override
    public String getFormattedMessage() {
        return delegate().getFormattedMessage();
    }

    @Override
    public String getLoggerName() {
        return delegate().getLoggerName();
    }

    @Override
    public LoggerContextVO getLoggerContextVO() {
        return delegate().getLoggerContextVO();
    }

    @Override
    public IThrowableProxy getThrowableProxy() {
        return delegate().getThrowableProxy();
    }

    @Override
    public StackTraceElement[] getCallerData() {
        return delegate().getCallerData();
    }

    @Override
    public boolean hasCallerData() {
        return delegate().hasCallerData();
    }

    @Override
    public Marker getMarker() {
        return delegate().getMarker();
    }

    @Override
    public Map<String, String> getMDCPropertyMap() {
        return delegate().getMDCPropertyMap();
    }

    @SuppressWarnings("deprecation")
    @Override
    public Map<String, String> getMdc() {
        return delegate().getMdc();
    }

    @Override
    public long getTimeStamp() {
        return delegate().getTimeStamp();
    }

    @Override
    public void prepareForDeferredProcessing() {
        delegate().prepareForDeferredProcessing();
    }

    @Override
    public String toString() {
        return delegate().toString();
    }
}
