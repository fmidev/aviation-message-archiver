package fi.fmi.avi.archiver.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.IntUnaryOperator;
import java.util.stream.IntStream;

public final class LoggableTests {
    private LoggableTests() {
        throw new AssertionError();
    }

    public static void assertAppendingLoggableContract(final AppendingLoggable loggable) {
        final StringBuilder builder = new StringBuilder();
        loggable.appendTo(builder);
        assertThat(loggable.toString())//
                .as("toString() equal to appendTo() result")//
                .isEqualTo(builder.toString());
    }

    public static void assertDecentLengthEstimate(final Loggable loggable) {
        assertDecentLengthEstimate(loggable, length -> (int) (length * 1.5));
    }

    public static void assertDecentLengthEstimate(final Loggable loggable, final IntUnaryOperator maxLength) {
        final int result = loggable.estimateLogStringLength();
        final int actual = loggable.toString().length();

        assertThat(result)//
                .isGreaterThanOrEqualTo(actual)//
                .isLessThan(maxLength.applyAsInt(actual));
    }

    public static String createString(final int length) {
        return createString(length, i -> 'a');
    }

    public static String createString(final int length, final IntUnaryOperator indexToChar) {
        return IntStream.range(0, length)//
                .collect(() -> new StringBuilder(length), (builder, codePoint) -> builder.append((char) indexToChar.applyAsInt(codePoint)),
                        StringBuilder::append)//
                .toString();
    }
}
