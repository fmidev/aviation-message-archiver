package fi.fmi.avi.archiver.spring.retry;

import org.apache.commons.logging.Log;
import org.springframework.retry.RetryListener;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.BackOffPolicy;
import org.springframework.retry.policy.RetryContextCache;
import org.springframework.retry.support.RetryTemplate;

/**
 * An abstract class delegating public methods of {@link RetryTemplate} to a delegate instance.
 */
public abstract class ForwardingRetryTemplate extends RetryTemplate {
    protected abstract RetryTemplate delegate();

    @Override
    public void setThrowLastExceptionOnExhausted(final boolean throwLastExceptionOnExhausted) {
        delegate().setThrowLastExceptionOnExhausted(throwLastExceptionOnExhausted);
    }

    @Override
    public void setRetryContextCache(final RetryContextCache retryContextCache) {
        delegate().setRetryContextCache(retryContextCache);
    }

    @Override
    public void setListeners(final RetryListener[] listeners) {
        delegate().setListeners(listeners);
    }

    @Override
    public void registerListener(final RetryListener listener) {
        delegate().registerListener(listener);
    }

    @Override
    public void registerListener(final RetryListener listener, final int index) {
        delegate().registerListener(listener, index);
    }

    @Override
    public boolean hasListeners() {
        return delegate().hasListeners();
    }

    @Override
    public void setLogger(final Log logger) {
        delegate().setLogger(logger);
    }

    @Override
    public void setBackOffPolicy(final BackOffPolicy backOffPolicy) {
        delegate().setBackOffPolicy(backOffPolicy);
    }

    @Override
    public void setRetryPolicy(final RetryPolicy retryPolicy) {
        delegate().setRetryPolicy(retryPolicy);
    }


}
