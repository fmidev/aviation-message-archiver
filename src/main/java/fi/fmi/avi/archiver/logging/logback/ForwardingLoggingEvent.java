package fi.fmi.avi.archiver.logging.logback;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.LoggerContextVO;
import org.slf4j.Marker;
import org.slf4j.event.KeyValuePair;

import java.time.Instant;
import java.util.List;
import java.util.Map;

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

    @SuppressWarnings("deprecation")
    @Deprecated
    @Override
    public Marker getMarker() {
        return delegate().getMarker();
    }

    @Override
    public List<Marker> getMarkerList() {
        return delegate().getMarkerList();
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
    public int getNanoseconds() {
        return delegate().getNanoseconds();
    }

    @Override
    public Instant getInstant() {
        return delegate().getInstant();
    }

    @Override
    public long getSequenceNumber() {
        return delegate().getSequenceNumber();
    }

    @Override
    public List<KeyValuePair> getKeyValuePairs() {
        return delegate().getKeyValuePairs();
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
