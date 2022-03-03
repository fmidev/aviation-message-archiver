package fi.fmi.avi.archiver.logging.logback.logstash;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.logstash.logback.argument.StructuredArguments;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.google.auto.value.AutoValue;
import com.google.common.testing.EqualsTester;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.spi.AppenderAttachableImpl;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fi.fmi.avi.archiver.logging.AbstractLoggable;
import fi.fmi.avi.archiver.logging.StructuredLoggable;
import fi.fmi.avi.archiver.logging.logback.ForwardingAppenderBaseTester;
import fi.fmi.avi.archiver.logging.logback.ForwardingLoggingEvent;

@SuppressFBWarnings({ "SIC_INNER_SHOULD_BE_STATIC", "UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR" })
final class StructuredLoggableLogstashAppenderTest {
    private static TestStructuredLoggable structured(final String name, final String value) {
        return TestStructuredLoggable.create(name, value);
    }

    @SuppressWarnings("unchecked")
    private static <T> Consumer<T>[] sameElementsAs(@Nullable final T[] expected) {
        if (expected == null) {
            return (Consumer<T>[]) new Consumer[0];
        }
        return Arrays.stream(expected)//
                .<Consumer<T>> map(expectedElement -> elementUnderTest -> assertThat(elementUnderTest).isSameAs(expectedElement))//
                .toArray(Consumer[]::new);
    }

    @AutoValue
    static abstract class TestStructuredLoggable extends AbstractLoggable implements StructuredLoggable {
        static TestStructuredLoggable create(final String name, final String value) {
            return new AutoValue_StructuredLoggableLogstashAppenderTest_TestStructuredLoggable(name, value, 0);
        }

        @Override
        public int estimateLogStringLength() {
            return toString().length();
        }

        @Override
        public StructuredLoggable readableCopy() {
            return new AutoValue_StructuredLoggableLogstashAppenderTest_TestStructuredLoggable(getStructureName(), getValue(), getGeneration() + 1);
        }

        @Override
        public abstract String getStructureName();

        public abstract String getValue();

        public abstract int getGeneration();
    }

    @Nested
    final class AppenderTest {
        @Mock
        private AppenderAttachableImpl<ILoggingEvent> appenderAttachable;
        private AutoCloseable mocks;

        private StructuredLoggableLogstashAppender appender;

        @BeforeEach
        void setUp() {
            mocks = MockitoAnnotations.openMocks(this);

            when(appenderAttachable.iteratorForAppenders()).thenReturn(Collections.emptyIterator());

            appender = new StructuredLoggableLogstashAppender(appenderAttachable);
            appender.start();
            clearInvocations(appenderAttachable);
        }

        @AfterEach
        void tearDown() throws Exception {
            if (mocks != null) {
                mocks.close();
            }
        }

        @Test
        void appender_is_initially_stopped() {
            appender = new StructuredLoggableLogstashAppender(appenderAttachable);
            assertThat(appender.isStarted()).isFalse();
        }

        @Test
        void append_does_nothing_when_stopped() {
            final ILoggingEvent loggingEvent = mock(ILoggingEvent.class);

            appender.stop();
            appender.append(loggingEvent);

            verify(appenderAttachable, never()).appendLoopOnAppenders(any());
        }

        @Test
        void append_given_null_does_nothing() {
            appender.append(null);

            verify(appenderAttachable, never()).appendLoopOnAppenders(any());
        }

        @Test
        void append_wraps_event_and_delegates_to_AppenderAttachable() {
            final ILoggingEvent loggingEvent = mock(ILoggingEvent.class);
            final Object[] argumentArray = new Object[] { "string arg" };
            when(loggingEvent.getArgumentArray()).thenReturn(argumentArray);

            appender.append(loggingEvent);

            verify(appenderAttachable).appendLoopOnAppenders(isA(StructuredLoggableLogstashAppender.LoggingEvent.class));
            verify(appenderAttachable).appendLoopOnAppenders(argThat(argument -> Arrays.equals(argumentArray, argument.getArgumentArray())));
        }
    }

    @Nested
    final class ForwardingAppenderTest extends ForwardingAppenderBaseTester<ILoggingEvent, AppenderAttachableImpl<ILoggingEvent>> {
        @Override
        protected StructuredLoggableLogstashAppender createAppender(final AppenderAttachableImpl<ILoggingEvent> delegateAppender) {
            return new StructuredLoggableLogstashAppender(delegateAppender);
        }

        @Override
        protected void append_delegates_to_appenders() {
            // Not applicable: overridden. Ignore.
        }
    }

    @Nested
    final class LoggingEventTest {
        @Mock
        private ILoggingEvent delegate;
        private AutoCloseable mocks;

        private StructuredLoggableLogstashAppender.LoggingEvent loggingEvent;
        private TestStructuredLoggable structured1;
        private TestStructuredLoggable structured2;
        private Object[] initialArgs;

        @BeforeEach
        void setUp() {
            mocks = MockitoAnnotations.openMocks(this);

            structured1 = structured("struct1", "structured value 1");
            structured2 = structured("struct2", "structured value 2");
            initialArgs = new Object[] { //
                    "string arg", //
                    structured1, //
                    structured2, //
                    3, //
            };

            loggingEvent = new StructuredLoggableLogstashAppender.LoggingEvent(delegate);
        }

        @AfterEach
        void tearDown() throws Exception {
            if (mocks != null) {
                mocks.close();
            }
        }

        @Test
        void is_ForwardingLoggingEvent() {
            assertThat(loggingEvent).isInstanceOf(ForwardingLoggingEvent.class);
        }

        @Test
        void getArgumentArray_returns_null_when_delegate_returns_null() {
            when(delegate.getArgumentArray()).thenReturn(null);

            assertThat(loggingEvent.getArgumentArray()).isNull();
        }

        @Test
        void getArgumentArray_returns_empty_when_delegate_returns_empty() {
            final Object[] emptyArgs = new Object[0];
            when(delegate.getArgumentArray()).thenReturn(emptyArgs);

            assertThat(loggingEvent.getArgumentArray()).isEqualTo(emptyArgs);
        }

        @Test
        void getArgumentArray_returns_argumentArray_equal_to_delegates_when_it_contains_no_StructuredLoggables() {
            final Object[] args = new Object[] { "string arg", 17, new Object(), StructuredArguments.keyValue("test", new Object()) };
            when(delegate.getArgumentArray()).thenReturn(args);

            assertThat(loggingEvent.getArgumentArray()).isEqualTo(args);
        }

        @Test
        void getArgumentArray_returns_argumentArray_with_StructuredLoggables_wrapped_as_StructuredArguments() {
            when(delegate.getArgumentArray()).thenReturn(initialArgs);

            final Object[] result = loggingEvent.getArgumentArray();

            final Object[] expectedArgs = new Object[] { //
                    "string arg", //
                    StructuredArguments.keyValue("struct1", structured1), //
                    StructuredArguments.keyValue("struct2", structured2), //
                    3, //
            };
            assertThat(result).isEqualTo(expectedArgs);
        }

        @Test
        void getArgumentArray_after_prepareForDeferredProcessing_returns_argumentArray_with_copies_of_StructuredLoggables_wrapped_as_StructuredArguments() {
            when(delegate.getArgumentArray()).thenReturn(initialArgs);

            loggingEvent.prepareForDeferredProcessing();
            final Object[] result = loggingEvent.getArgumentArray();

            final Object[] expectedArgs = new Object[] { //
                    "string arg", //
                    StructuredArguments.keyValue("struct1", structured1.readableCopy()), //
                    StructuredArguments.keyValue("struct2", structured2.readableCopy()), //
                    3, //
            };
            assertThat(result).isEqualTo(expectedArgs);
        }

        @Test
        void getArgumentArray_after_prepareForDeferredProcessing_returns_argumentArray_with_copies_of_StructuredLoggables_wrapped_as_StructuredArguments2() {
            when(delegate.getArgumentArray()).thenReturn(initialArgs);

            final Object[] resultBeforeDefer = loggingEvent.getArgumentArray();
            final Object[] expectedArgsBeforeDefer = new Object[] { //
                    "string arg", //
                    StructuredArguments.keyValue("struct1", structured1), //
                    StructuredArguments.keyValue("struct2", structured2), //
                    3, //
            };
            assertThat(resultBeforeDefer).as("before defer").isEqualTo(expectedArgsBeforeDefer);

            loggingEvent.prepareForDeferredProcessing();
            final Object[] resultAfterDefer = loggingEvent.getArgumentArray();

            final Object[] expectedArgsAfterDefer = new Object[] { //
                    "string arg", //
                    StructuredArguments.keyValue("struct1", structured1.readableCopy()), //
                    StructuredArguments.keyValue("struct2", structured2.readableCopy()), //
                    3, //
            };
            assertThat(resultAfterDefer).as("after defer").isEqualTo(expectedArgsAfterDefer);
            assertThat(resultAfterDefer).as("after defer not equal to before").isNotEqualTo(expectedArgsBeforeDefer);
        }

        @Test
        void getArgumentArray_does_not_affect_delegate() {
            final Object[] delegateArgumentArray = initialArgs.clone();
            when(delegate.getArgumentArray()).thenReturn(delegateArgumentArray);

            final Object[] result1 = loggingEvent.getArgumentArray();
            loggingEvent.prepareForDeferredProcessing();
            final Object[] result2 = loggingEvent.getArgumentArray();

            assertThat(delegateArgumentArray).as("delegate argumentArray is unchanged").isEqualTo(initialArgs);
            assertThat(result1).as("result1 and delegateArgumentArray").isNotSameAs(delegateArgumentArray);
            assertThat(result2).as("result2 and delegateArgumentArray").isNotSameAs(delegateArgumentArray);
        }

        @Test
        void getArgumentArray_returns_same_array_on_every_invocation() {
            when(delegate.getArgumentArray()).thenReturn(initialArgs);

            final Object[] result1 = loggingEvent.getArgumentArray();
            final Object[] result2 = loggingEvent.getArgumentArray();
            loggingEvent.prepareForDeferredProcessing();
            final Object[] result3 = loggingEvent.getArgumentArray();
            final Object[] result4 = loggingEvent.getArgumentArray();

            assertThat(result2).as("result2 and result1").isSameAs(result1);
            assertThat(result3).as("result3 and result1").isSameAs(result1);
            assertThat(result4).as("result4 and result1").isSameAs(result1);
        }

        @SuppressWarnings("ConstantConditions")
        @Test
        void getArgumentArray_constructs_values_only_once() {
            when(delegate.getArgumentArray()).thenReturn(initialArgs);

            final Object[] result1 = loggingEvent.getArgumentArray().clone();
            final Object[] result2 = loggingEvent.getArgumentArray().clone();
            loggingEvent.prepareForDeferredProcessing();
            final Object[] result3 = loggingEvent.getArgumentArray().clone();
            final Object[] result4 = loggingEvent.getArgumentArray().clone();
            loggingEvent.prepareForDeferredProcessing();
            final Object[] result5 = loggingEvent.getArgumentArray().clone();

            assertThat(result2).as("results before defer").satisfiesExactly(sameElementsAs(result1));
            assertThat(result4).as("results after defer").satisfiesExactly(sameElementsAs(result3));
            assertThat(result5).as("results after second defer").satisfiesExactly(sameElementsAs(result3));
        }
    }

    @Nested
    final class TestStructuredLoggableTest {
        @SuppressWarnings("UnstableApiUsage")
        @Test
        void testEquals() {
            final TestStructuredLoggable structured = structured("structured", "structured value");
            new EqualsTester()//
                    .addEqualityGroup(structured, structured, structured("structured", "structured value"))//
                    .addEqualityGroup(structured("structured2", "structured value"))//
                    .addEqualityGroup(structured("structured", "alternate structured value"))//
                    .addEqualityGroup(structured.readableCopy(), structured.readableCopy())//
                    .testEquals();
        }
    }
}
