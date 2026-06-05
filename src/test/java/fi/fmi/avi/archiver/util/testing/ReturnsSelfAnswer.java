package fi.fmi.avi.archiver.util.testing;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.lang.reflect.Method;

import static java.util.Objects.requireNonNull;
import static org.mockito.Mockito.RETURNS_DEFAULTS;

class ReturnsSelfAnswer implements Answer<Object> {
    static final ReturnsSelfAnswer DEFAULT_INSTANCE = new ReturnsSelfAnswer(RETURNS_DEFAULTS);

    private final Answer<?> fallback;

    ReturnsSelfAnswer(final Answer<?> fallback) {
        this.fallback = requireNonNull(fallback, "fallback");
    }

    @Override
    public Object answer(final InvocationOnMock invocation) throws Throwable {
        requireNonNull(invocation, "invocation");
        final Method method = invocation.getMethod();
        final Class<?> returnType = method.getReturnType();
        final Object mock = invocation.getMock();

        if (method.getDeclaringClass() == Object.class) {
            return fallback.answer(invocation);
        } else if (returnType.isInstance(mock) || returnType == Object.class) {
            return mock;
        } else {
            return fallback.answer(invocation);
        }
    }
}
