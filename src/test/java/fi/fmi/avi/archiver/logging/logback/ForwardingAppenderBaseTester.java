package fi.fmi.avi.archiver.logging.logback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import ch.qos.logback.core.Appender;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.spi.AppenderAttachable;

public abstract class ForwardingAppenderBaseTester<E, A extends AppenderAttachable<E>> {
    private A delegateAppender;
    private ForwardingAppenderBase<E> appender;

    @SuppressWarnings("unchecked")
    @Nullable
    static <T> T dummyValue(final Class<T> cls) {
        if (void.class.isAssignableFrom(cls)) {
            return null;
        } else if (boolean.class.isAssignableFrom(cls)) {
            return (T) Boolean.TRUE;
        } else if (String.class.isAssignableFrom(cls)) {
            return (T) "";
        } else {
            return mock(cls);
        }
    }

    @SuppressWarnings("unchecked")
    @BeforeEach
    final void setUpForwardingAppenderBaseTester() {
        delegateAppender = mock(delegateAppenderType());

        when(delegateAppender.iteratorForAppenders()).thenReturn(Collections.emptyIterator());

        appender = createAppender(delegateAppender);
        appender.start();
        clearInvocations(delegateAppender);
    }

    @SuppressWarnings("unchecked")
    private Class<E> eventType() {
        return (Class<E>) ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    }

    @SuppressWarnings("unchecked")
    private Class<A> delegateAppenderType() {
        return (Class<A>) ((ParameterizedType) ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[1]).getRawType();
    }

    protected abstract ForwardingAppenderBase<E> createAppender(A delegateAppender);

    @Test
    protected void start_sets_state_as_started() {
        appender.start();

        assertThat(appender.isStarted()).isTrue();
    }

    @Test
    protected void start_invokes_start_on_each_attached_appender_only_when_it_is_not_started() {
        @SuppressWarnings("unchecked")
        final List<Appender<E>> attachedAppenders = Arrays.asList(mock(Appender.class), mock(Appender.class), mock(Appender.class));
        when(attachedAppenders.get(0).isStarted()).thenReturn(false);
        when(attachedAppenders.get(1).isStarted()).thenReturn(true);
        when(attachedAppenders.get(2).isStarted()).thenReturn(false);
        when(delegateAppender.iteratorForAppenders()).thenAnswer(invocation -> attachedAppenders.iterator());

        appender.start();

        verify(attachedAppenders.get(0)).isStarted();
        verify(attachedAppenders.get(0)).start();
        verify(attachedAppenders.get(1)).isStarted();
        verify(attachedAppenders.get(1), never()).start();
        verify(attachedAppenders.get(2)).isStarted();
        verify(attachedAppenders.get(2)).start();
    }

    @Test
    protected void start_sets_context_on_attached_if_missing() {
        final Context appenderContext = mock(Context.class);
        appender.setContext(appenderContext);
        final Context attachedContext1 = mock(Context.class);
        @SuppressWarnings("unchecked")
        final List<Appender<E>> attachedAppenders = Arrays.asList(mock(Appender.class), mock(Appender.class), mock(Appender.class));
        when(attachedAppenders.get(0).getContext()).thenReturn(null);
        when(attachedAppenders.get(1).getContext()).thenReturn(attachedContext1);
        when(attachedAppenders.get(2).getContext()).thenReturn(null);
        when(delegateAppender.iteratorForAppenders()).thenAnswer(invocation -> attachedAppenders.iterator());

        appender.start();

        verify(attachedAppenders.get(0)).getContext();
        verify(attachedAppenders.get(0)).setContext(appenderContext);
        verify(attachedAppenders.get(1)).getContext();
        verify(attachedAppenders.get(1), never()).setContext(any());
        verify(attachedAppenders.get(2)).getContext();
        verify(attachedAppenders.get(2)).setContext(appenderContext);
    }

    @Test
    protected void stop_sets_state_as_not_started() {
        appender.stop();

        assertThat(appender.isStarted()).isFalse();
    }

    @Test
    protected void stop_invokes_stop_on_each_attached_appender_only_when_it_is_started() {
        @SuppressWarnings("unchecked")
        final List<Appender<E>> attachedAppenders = Arrays.asList(mock(Appender.class), mock(Appender.class), mock(Appender.class));
        when(attachedAppenders.get(0).isStarted()).thenReturn(true);
        when(attachedAppenders.get(1).isStarted()).thenReturn(false);
        when(attachedAppenders.get(2).isStarted()).thenReturn(true);
        when(delegateAppender.iteratorForAppenders()).thenAnswer(invocation -> attachedAppenders.iterator());

        appender.stop();

        verify(attachedAppenders.get(0)).isStarted();
        verify(attachedAppenders.get(0)).stop();
        verify(attachedAppenders.get(1)).isStarted();
        verify(attachedAppenders.get(1), never()).stop();
        verify(attachedAppenders.get(2)).isStarted();
        verify(attachedAppenders.get(2)).stop();
    }

    @Test
    protected void append_does_nothing_when_stopped() {
        @SuppressWarnings("unchecked")
        final List<Appender<E>> attachedAppenders = Arrays.asList(mock(Appender.class), mock(Appender.class), mock(Appender.class));
        when(delegateAppender.iteratorForAppenders()).thenAnswer(invocation -> attachedAppenders.iterator());
        final E loggingEvent = mock(eventType());

        appender.stop();
        appender.append(loggingEvent);

        attachedAppenders.forEach(attachedAppender -> verify(attachedAppender, never()).doAppend(any()));
    }

    @Test
    protected void append_given_null_does_nothing() {
        @SuppressWarnings("unchecked")
        final List<Appender<E>> attachedAppenders = Arrays.asList(mock(Appender.class), mock(Appender.class), mock(Appender.class));
        when(delegateAppender.iteratorForAppenders()).thenAnswer(invocation -> attachedAppenders.iterator());

        appender.append(null);

        attachedAppenders.forEach(attachedAppender -> verify(attachedAppender, never()).doAppend(any()));
    }

    @Test
    protected void append_delegates_to_appenders() {
        @SuppressWarnings("unchecked")
        final List<Appender<E>> attachedAppenders = Arrays.asList(mock(Appender.class), mock(Appender.class), mock(Appender.class));
        when(delegateAppender.iteratorForAppenders()).thenAnswer(invocation -> attachedAppenders.iterator());
        final E loggingEvent = mock(eventType());

        appender.append(loggingEvent);

        attachedAppenders.forEach(attachedAppender -> verify(attachedAppender).doAppend(loggingEvent));
    }

    @ParameterizedTest
    @ArgumentsSource(AppenderAttachableMethodsProvider.class)
    protected void testForwardingToAppenderAttachable(final Method method) throws InvocationTargetException, IllegalAccessException {
        final Object expectedReturnValue = dummyValue(method.getReturnType());
        final Object[] parameters = Arrays.stream(method.getParameterTypes())//
                .map(ForwardingAppenderBaseTester::dummyValue)//
                .toArray();
        if (!void.class.isAssignableFrom(method.getReturnType())) {
            method.invoke(doReturn(expectedReturnValue).when(delegateAppender), parameters);
        }

        final Object result = method.invoke(appender, parameters);

        method.invoke(verify(delegateAppender), parameters);
        assertThat(result).isEqualTo(expectedReturnValue);
    }

    protected static class AppenderAttachableMethodsProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(final ExtensionContext context) {
            return Arrays.stream(AppenderAttachable.class.getMethods())//
                    .map(Arguments::of);
        }
    }
}
