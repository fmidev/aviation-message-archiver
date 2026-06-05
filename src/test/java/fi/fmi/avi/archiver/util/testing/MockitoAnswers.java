package fi.fmi.avi.archiver.util.testing;

import org.mockito.stubbing.Answer;

import static java.util.Objects.requireNonNull;

public final class MockitoAnswers {
    private MockitoAnswers() {
        throw new AssertionError();
    }

    /**
     * Returns an answer that returns the mock itself whenever the invoked method's return type
     * is either assignable from the mock's type, <em>or</em> is {@code Object} (the case of methods
     * inherited from a self-referential generic super-interface, e.g. {@code T myMethod(String)}).
     * Falls back to the Mockito default answer otherwise.
     *
     * <p>
     * Excludes {@code Object}-returning methods declared directly on {@link Object} itself
     * (toString, equals, hashCode, ...).
     * </p>
     *
     * <p>
     * This answer is similar to {@link org.mockito.Mockito#RETURNS_SELF}, but the Mockitos default implementation always
     * returns default values for methods with {@code Object} as the raw return type after type erasure.
     * </p>
     *
     * @return answer returning the mock itself, with default fallback
     */
    public static Answer<Object> returnsSelf() {
        return ReturnsSelfAnswer.DEFAULT_INSTANCE;
    }

    /**
     * Like {@link #returnsSelf()}, but falls back to the provided answer instead of the Mockito default answer.
     *
     * @param fallback fallback answer
     * @return answer returning the mock itself, with a custom fallback
     */
    public static Answer<Object> returnsSelf(final Answer<?> fallback) {
        return new ReturnsSelfAnswer(requireNonNull(fallback, "fallback"));
    }
}
