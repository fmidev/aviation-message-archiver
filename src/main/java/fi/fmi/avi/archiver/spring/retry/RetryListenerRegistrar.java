package fi.fmi.avi.archiver.spring.retry;

import org.springframework.retry.RetryListener;

interface RetryListenerRegistrar {
    void registerListener(RetryListener listener);
}
