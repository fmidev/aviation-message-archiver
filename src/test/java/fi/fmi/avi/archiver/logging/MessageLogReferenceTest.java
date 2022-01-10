package fi.fmi.avi.archiver.logging;

import static fi.fmi.avi.archiver.logging.MessageLogReference.builder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

final class MessageLogReferenceTest {
    @Test
    void messageIndex_below_zero_is_forbidden() {
        assertThatIllegalArgumentException().isThrownBy(() -> builder().setMessageIndex(-1));
    }

    @Test
    void estimateLogStringLength_returns_decent_estimate() {
        final MessageLogReference messageLogReference = builder()//
                .setMessageIndex(100)//
                .setMessageContent(LoggableTests.createString(100))//
                .build();
        LoggableTests.assertDecentLengthEstimate(messageLogReference);
    }

    @ArgumentsSource(TestValuesProvider.class)
    @ParameterizedTest
    void logString_is_expected(final String expectedStringTemplate, @Nullable final String expectedExcerpt, final MessageLogReference messageLogReference) {
        final String expectedString = expectedExcerpt == null ? expectedStringTemplate : expectedStringTemplate.replace("%s", expectedExcerpt);
        assertThat(messageLogReference.toString()).isEqualTo(expectedString);
    }

    @ArgumentsSource(TestValuesProvider.class)
    @ParameterizedTest
    void messageExcerpt_is_expected(final String expectedStringTemplate, @Nullable final String expectedExcerpt,
            final MessageLogReference messageLogReference) {
        assertThat(messageLogReference.getMessageExcerpt().orElse(null)).isEqualTo(expectedExcerpt);
    }

    @Test
    void getMessageExcerpt_returns_empty_when_getMessageContent_returns_empty() {
        final MessageLogReference messageLogReference = builder()//
                .clearMessageContent()//
                .buildPartial();
        assertThat(messageLogReference.getMessageExcerpt()).isEmpty();
    }

    static class TestValuesProvider implements ArgumentsProvider {
        private static Arguments args(final String expectedStringTemplate, @Nullable final String expectedExcerpt,
                final MessageLogReference messageLogReference) {
            return Arguments.of(expectedStringTemplate, expectedExcerpt, messageLogReference);
        }

        @Override
        public Stream<Arguments> provideArguments(final ExtensionContext context) {
            return Stream.of(//
                    args("1", null, MessageLogReference.builder()//
                            .setMessageIndex(0)//
                            .build()), //
                    args("2", null, MessageLogReference.builder()//
                            .setMessageIndex(1)//
                            .clearMessageContent()//
                            .build()), //
                    args("4(%s)", "message content", MessageLogReference.builder()//
                            .setMessageIndex(3)//
                            .setMessageContent("message content")//
                            .build()), //
                    args("6(%s)", "Overlong message content with ?œø? char...", MessageLogReference.builder()//
                            .setMessageIndex(5)//
                            .setMessageContent("Overlong \n message \t\t content     with €œø\u0000\u0888 characters and extra whitespaces")//
                            .build()), //
                    args("7(%s)", "my-test-iwxxm-document-id", MessageLogReference.builder()//
                            .setMessageIndex(6)//
                            .setMessageContent(//
                                    "<iwxxm:iwxxmMessage " //
                                            + "attr1=\"value1\" \n\t" //
                                            + "iwxxm:attr2=\"value2\"   " //
                                            + "ns1:id=\"my-test-iwxxm-document-id\" " //
                                            + "ns2:attr3=\"value3\" >" //
                                            + "\n\t...\n" //
                                            + "</iwxxm:iwxxmMessage>")//
                            .build()), //
                    args("8(%s)", "my-?very??-#!?œø?-overly-long-test-@iwx...", MessageLogReference.builder()//
                            .setMessageIndex(7)//
                            .setMessageContent(//
                                    "<iwxxmMessage id=\"my-(very:)-#!€œø\u0000\u0888-overly-long-test-@iwxxm-document-id-of-plain-crap\">" //
                                            + "\n\t...\n" //
                                            + "</iwxxmMessage>")//
                            .build()), //
                    args("9(%s)", "min-msg", MessageLogReference.builder()//
                            .setMessageIndex(8)//
                            .setMessageContent("<minimalMsg id=\"min-msg\" />")//
                            .build()), //
                    args("10(%s)", "?any uid=\"123\"?...?/any?", MessageLogReference.builder()//
                            .setMessageIndex(9)//
                            .setMessageContent("<any uid=\"123\">...</any>")//
                            .build()));
        }
    }
}
